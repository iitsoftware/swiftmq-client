/*
 * Copyright 2019 IIT Software GmbH
 *
 * IIT Software GmbH licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.swiftmq.admin.cli;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal, dependency-free line editor for the CLI. The mode is chosen once at construction:
 * <ul>
 * <li>RAW: Unix/macOS with a TTY on stdin. The terminal is switched to raw mode via {@code stty}
 * for the duration of each read only; editing keys and an in-memory history are handled here.</li>
 * <li>CONSOLE: Windows with a console. Delegates to {@link Console}, which provides history itself.</li>
 * <li>PLAIN: no TTY (piped input). Reads lines from stdin, never emits ANSI sequences.</li>
 * </ul>
 * Known limitations in RAW mode: the terminal width is not queried, so lines wider than the terminal
 * (prompt + input) are not redrawn correctly. Every code point is assumed to occupy one column, so the
 * cursor is misplaced after double-width characters (CJK, most emoji).
 * <p>
 * Other threads print asynchronous messages through {@link #printAbove(String)} so they don't
 * end up in the middle of the line being edited.
 */
public class LineEditor {
    private enum Mode {RAW, CONSOLE, PLAIN}

    private static final int MAX_HISTORY = 500;
    private static final int CTRL_A = 0x01, CTRL_C = 0x03, CTRL_D = 0x04, CTRL_E = 0x05;
    private static final int BS = 0x08, CTRL_U = 0x15, ESC = 0x1B, DEL = 0x7F;
    // Synthetic key codes for escape sequences, outside the char range
    private static final int KEY_NONE = 0x10000, KEY_UP = 0x10001, KEY_DOWN = 0x10002, KEY_LEFT = 0x10003,
            KEY_RIGHT = 0x10004, KEY_HOME = 0x10005, KEY_END = 0x10006, KEY_DELETE = 0x10007;
    private static final File TTY = new File("/dev/tty");

    private final Mode mode;
    private final String savedTtyState;
    private final List<String> history = new ArrayList<>();
    private Reader rawReader;
    private BufferedReader plainReader;
    private volatile boolean rawActive = false;
    // State of the line being edited in RAW mode, guarded by this. editPrompt is non-null while a read is in progress
    private final StringBuilder editBuf = new StringBuilder();
    private String editPrompt = null;
    private int editCursor = 0;
    private boolean editMask = false;

    public LineEditor() {
        if (System.getProperty("os.name", "").startsWith("Windows")) {
            savedTtyState = null;
            mode = System.console() != null ? Mode.CONSOLE : Mode.PLAIN;
        } else {
            // System.console() is not used for TTY detection: its behavior changed between JDK 22 and 25
            savedTtyState = queryTtyState();
            mode = savedTtyState != null ? Mode.RAW : Mode.PLAIN;
        }
        if (mode == Mode.RAW) {
            rawReader = new InputStreamReader(System.in, StandardCharsets.UTF_8);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (rawActive)
                    stty(savedTtyState);
            }));
        } else if (mode == Mode.PLAIN)
            plainReader = new BufferedReader(new InputStreamReader(System.in));
    }

    public String readLine(String prompt) throws IOException {
        return read(prompt, false);
    }

    public String readPassword(String prompt) throws IOException {
        return read(prompt, true);
    }

    /**
     * Prints a message from another thread. In RAW mode with a read in progress, the input line is
     * cleared, the message printed and the prompt plus the typed text (masked for passwords) redrawn
     * below it with the cursor restored. Otherwise the message is simply printed.
     *
     * @param msg the message
     */
    public synchronized void printAbove(String msg) {
        if (editPrompt == null) {
            System.out.println(msg);
            return;
        }
        System.out.print("\r\u001B[K" + msg + "\n");
        redraw();
    }

    private String read(String prompt, boolean mask) throws IOException {
        switch (mode) {
            case CONSOLE:
                if (mask) {
                    char[] pw = System.console().readPassword("%s", prompt);
                    return pw == null ? null : new String(pw);
                }
                return System.console().readLine("%s", prompt);
            case PLAIN:
                System.out.print(prompt);
                System.out.flush();
                return plainReader.readLine();
            default:
                rawActive = true;
                stty("-icanon", "-echo", "-isig", "min", "1");
                try {
                    return edit(prompt, mask);
                } finally {
                    stty(savedTtyState);
                    rawActive = false;
                }
        }
    }

    private String edit(String prompt, boolean mask) throws IOException {
        int histIdx = history.size();
        String pending = "";
        synchronized (this) {
            editPrompt = prompt;
            editMask = mask;
            editBuf.setLength(0);
            editCursor = 0;
            redraw();
        }
        try {
            while (true) {
                // Block outside the lock so printAbove can run while waiting for a key
                int c = readKey();
                // Keep surrogate pairs (e.g. emoji) together so they are never split by editing
                int low = c < KEY_NONE && Character.isHighSurrogate((char) c) ? rawReader.read() : -1;
                synchronized (this) {
                    switch (c) {
                        case -1:
                        case CTRL_C:
                            return endEdit(null);
                        case '\r':
                        case '\n':
                            String line = editBuf.toString();
                            if (!editMask)
                                addHistory(line);
                            return endEdit(line);
                        case CTRL_D:
                            if (editBuf.length() == 0)
                                return endEdit(null);
                            if (editCursor < editBuf.length())
                                editBuf.delete(editCursor, editBuf.offsetByCodePoints(editCursor, 1));
                            break;
                        case KEY_DELETE:
                            if (editCursor < editBuf.length())
                                editBuf.delete(editCursor, editBuf.offsetByCodePoints(editCursor, 1));
                            break;
                        case DEL:
                        case BS:
                            if (editCursor > 0) {
                                int start = editBuf.offsetByCodePoints(editCursor, -1);
                                editBuf.delete(start, editCursor);
                                editCursor = start;
                            }
                            break;
                        case KEY_LEFT:
                            if (editCursor > 0)
                                editCursor = editBuf.offsetByCodePoints(editCursor, -1);
                            break;
                        case KEY_RIGHT:
                            if (editCursor < editBuf.length())
                                editCursor = editBuf.offsetByCodePoints(editCursor, 1);
                            break;
                        case CTRL_A:
                        case KEY_HOME:
                            editCursor = 0;
                            break;
                        case CTRL_E:
                        case KEY_END:
                            editCursor = editBuf.length();
                            break;
                        case CTRL_U:
                            editBuf.delete(0, editCursor);
                            editCursor = 0;
                            break;
                        case KEY_UP:
                            if (editMask || histIdx == 0)
                                break;
                            if (histIdx == history.size())
                                pending = editBuf.toString();
                            editBuf.setLength(0);
                            editBuf.append(history.get(--histIdx));
                            editCursor = editBuf.length();
                            break;
                        case KEY_DOWN:
                            if (editMask || histIdx == history.size())
                                break;
                            histIdx++;
                            editBuf.setLength(0);
                            editBuf.append(histIdx == history.size() ? pending : history.get(histIdx));
                            editCursor = editBuf.length();
                            break;
                        default:
                            // Ignore remaining control characters and unmapped escape sequences
                            if (c < 0x20 || c >= KEY_NONE)
                                break;
                            editBuf.insert(editCursor++, (char) c);
                            if (low != -1)
                                editBuf.insert(editCursor++, (char) low);
                    }
                    redraw();
                }
            }
        } finally {
            synchronized (this) {
                editPrompt = null;
            }
        }
    }

    /**
     * Ends the read while still holding the lock, so printAbove never redraws a line already terminated.
     */
    private String endEdit(String result) {
        editPrompt = null;
        newline();
        return result;
    }

    private int readKey() throws IOException {
        int c = rawReader.read();
        if (c != ESC)
            return c;
        c = rawReader.read();
        if (c == '[') {
            // CSI: swallow parameter/intermediate bytes up to the final byte in 0x40-0x7E
            StringBuilder params = new StringBuilder();
            while ((c = rawReader.read()) != -1 && (c < 0x40 || c > 0x7E))
                params.append((char) c);
            return c == -1 ? -1 : mapSequence(c, params.toString());
        }
        if (c == 'O') {
            c = rawReader.read();
            return c == -1 ? -1 : mapSequence(c, "");
        }
        return c == -1 ? -1 : KEY_NONE;
    }

    private static int mapSequence(int finalByte, String params) {
        switch (finalByte) {
            case 'A':
                return KEY_UP;
            case 'B':
                return KEY_DOWN;
            case 'C':
                return KEY_RIGHT;
            case 'D':
                return KEY_LEFT;
            case 'H':
                return KEY_HOME;
            case 'F':
                return KEY_END;
            case '~':
                switch (params) {
                    case "1":
                        return KEY_HOME;
                    case "3":
                        return KEY_DELETE;
                    case "4":
                        return KEY_END;
                }
        }
        return KEY_NONE;
    }

    private void redraw() {
        StringBuilder out = new StringBuilder("\r").append(editPrompt);
        if (editMask)
            out.append("*".repeat(editBuf.codePointCount(0, editBuf.length())));
        else
            out.append(editBuf);
        out.append("\u001B[K");
        int back = editBuf.codePointCount(editCursor, editBuf.length());
        if (back > 0)
            out.append("\u001B[").append(back).append('D');
        System.out.print(out);
        System.out.flush();
    }

    private static void newline() {
        System.out.print('\n');
        System.out.flush();
    }

    private void addHistory(String line) {
        if (line.isEmpty() || (!history.isEmpty() && history.get(history.size() - 1).equals(line)))
            return;
        history.add(line);
        if (history.size() > MAX_HISTORY)
            history.remove(0);
    }

    /**
     * Runs {@code stty -g} with the inherited stdin. Exit code 0 means stdin is a TTY.
     *
     * @return the saved terminal state, or null if stdin is not a TTY
     */
    private static String queryTtyState() {
        try {
            Process p = new ProcessBuilder("stty", "-g")
                    .redirectInput(ProcessBuilder.Redirect.INHERIT)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            String state = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            return p.waitFor() == 0 && !state.isEmpty() ? state : null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    private static void stty(String... args) {
        List<String> cmd = new ArrayList<>();
        cmd.add("stty");
        cmd.addAll(List.of(args));
        try {
            new ProcessBuilder(cmd)
                    .redirectInput(TTY)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start()
                    .waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException ignored) {
        }
    }
}
