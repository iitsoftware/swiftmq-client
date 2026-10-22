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
        StringBuilder buf = new StringBuilder();
        int cursor = 0;
        int histIdx = history.size();
        String pending = "";
        redraw(prompt, buf, cursor, mask);
        while (true) {
            int c = readKey();
            switch (c) {
                case -1:
                case CTRL_C:
                    newline();
                    return null;
                case '\r':
                case '\n':
                    newline();
                    String line = buf.toString();
                    if (!mask)
                        addHistory(line);
                    return line;
                case CTRL_D:
                    if (buf.length() == 0) {
                        newline();
                        return null;
                    }
                    if (cursor < buf.length())
                        buf.delete(cursor, buf.offsetByCodePoints(cursor, 1));
                    break;
                case KEY_DELETE:
                    if (cursor < buf.length())
                        buf.delete(cursor, buf.offsetByCodePoints(cursor, 1));
                    break;
                case DEL:
                case BS:
                    if (cursor > 0) {
                        int start = buf.offsetByCodePoints(cursor, -1);
                        buf.delete(start, cursor);
                        cursor = start;
                    }
                    break;
                case KEY_LEFT:
                    if (cursor > 0)
                        cursor = buf.offsetByCodePoints(cursor, -1);
                    break;
                case KEY_RIGHT:
                    if (cursor < buf.length())
                        cursor = buf.offsetByCodePoints(cursor, 1);
                    break;
                case CTRL_A:
                case KEY_HOME:
                    cursor = 0;
                    break;
                case CTRL_E:
                case KEY_END:
                    cursor = buf.length();
                    break;
                case CTRL_U:
                    buf.delete(0, cursor);
                    cursor = 0;
                    break;
                case KEY_UP:
                    if (mask || histIdx == 0)
                        break;
                    if (histIdx == history.size())
                        pending = buf.toString();
                    buf.setLength(0);
                    buf.append(history.get(--histIdx));
                    cursor = buf.length();
                    break;
                case KEY_DOWN:
                    if (mask || histIdx == history.size())
                        break;
                    histIdx++;
                    buf.setLength(0);
                    buf.append(histIdx == history.size() ? pending : history.get(histIdx));
                    cursor = buf.length();
                    break;
                default:
                    // Ignore remaining control characters and unmapped escape sequences
                    if (c < 0x20 || c >= KEY_NONE)
                        break;
                    buf.insert(cursor++, (char) c);
                    // Keep surrogate pairs (e.g. emoji) together so they are never split by editing
                    if (Character.isHighSurrogate((char) c) && (c = rawReader.read()) != -1)
                        buf.insert(cursor++, (char) c);
            }
            redraw(prompt, buf, cursor, mask);
        }
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

    private void redraw(String prompt, StringBuilder buf, int cursor, boolean mask) {
        StringBuilder out = new StringBuilder("\r").append(prompt);
        if (mask)
            out.append("*".repeat(buf.codePointCount(0, buf.length())));
        else
            out.append(buf);
        out.append("\u001B[K");
        int back = buf.codePointCount(cursor, buf.length());
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
