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

package com.swiftmq.tools.file;

import java.io.*;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class GenerationalFileWriter extends Writer {
    static DecimalFormat FMTROTATE = new DecimalFormat("'-'000'.log'");
    final AtomicReference<String> directory = new AtomicReference<>();
    final AtomicReference<String> filename = new AtomicReference<>();
    final AtomicReference<File> file = new AtomicReference<>();
    final AtomicReference<FileWriter> writer = new AtomicReference<>();
    final AtomicLong length = new AtomicLong();
    final AtomicInteger generation = new AtomicInteger();
    final RolloverSizeProvider rolloverSizeProvider;
    final NumberGenerationProvider numberGenerationProvider;

    public GenerationalFileWriter(String directory, String filename, RolloverSizeProvider rolloverSizeProvider, NumberGenerationProvider numberGenerationProvider) throws IOException {
        this.directory.set(directory);
        this.filename.set(filename);
        this.rolloverSizeProvider = rolloverSizeProvider;
        this.numberGenerationProvider = numberGenerationProvider;
        newLogfile();
    }


    private void checkGenerations() throws IOException {
        File dir = new File(directory.get());
        if (!dir.exists()) {
            dir.mkdir();
            return;
        }
        String[] names = dir.list(new FilenameFilter() {
            public boolean accept(File file, String name) {
                return name.startsWith(filename.get()) && name.endsWith(".log");
            }
        });
        if (names != null) {
            Arrays.sort(names);
            for (int i = 0; i < names.length; i++) {
                int logIdx = names[i].indexOf(".log");
                if (logIdx - 3 >= 0) {
                    int g = Integer.parseInt(names[i].substring(logIdx - 3, logIdx));
                    generation.set(Math.max(g, generation.get()));
                }
            }
            int todDel = names.length - numberGenerationProvider.getNumberGenerations();
            if (generation.get() == 999) {
                generation.set(0);
                todDel = names.length;
            }
            if (todDel > 0) {
                for (int i = 0; i < todDel; i++) {
                    new File(dir, names[i]).delete();
                }
            }
        }
    }

    private void newLogfile() throws IOException {
        checkGenerations();
        file.set(new File(directory + File.separator + filename + FMTROTATE.format(generation.getAndIncrement())));
        if (file.get().exists())
            length.set(file.get().length());
        else
            length.set(0);
        writer.set(new FileWriter(file.get(), true));
    }

    private void checkRolling() throws IOException {
        long max = rolloverSizeProvider.getRollOverSize();
        if (max == -1)
            return;
        if (length.get() > max) {
            writer.get().flush();
            writer.get().close();
            newLogfile();
        }
    }

    public void write(char[] cbuf, int off, int len) throws IOException {
        if (writer.get() == null)
            return;
        writer.get().write(cbuf, off, len);
        writer.get().flush();
        length.addAndGet((len - off));
        checkRolling();
    }

    public void write(String str) throws IOException {
        if (writer.get() == null)
            return;
        writer.get().write(str);
        writer.get().flush();
        length.addAndGet(str.length());
        checkRolling();
    }

    public void write(String str, int off, int len) throws IOException {
        if (writer.get() == null)
            return;
        writer.get().write(str, off, len);
        writer.get().flush();
        length.addAndGet((len - off));
        checkRolling();
    }

    public void write(char[] cbuf) throws IOException {
        if (writer.get() == null)
            return;
        writer.get().write(cbuf);
        writer.get().flush();
        length.addAndGet(cbuf.length);
        checkRolling();
    }

    public void flush() throws IOException {
        if (writer.get() == null)
            return;
        writer.get().flush();
        checkRolling();
    }

    public void close() throws IOException {
        if (writer.get() == null)
            return;
        writer.get().flush();
        writer.get().close();
    }
}