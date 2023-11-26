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
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class RollingFileWriter extends Writer {
    static SimpleDateFormat FMTROTATE = new SimpleDateFormat("'-'yyyyMMddHHmmss'.old'");
    final AtomicReference<String> filename = new AtomicReference<>();
    final AtomicReference<File> file = new AtomicReference<>();
    final AtomicReference<String> directory = new AtomicReference<>();
    final AtomicReference<FileWriter> writer = new AtomicReference<>();
    final AtomicLong length = new AtomicLong();
    final AtomicInteger generation = new AtomicInteger();
    final AtomicReference<RolloverSizeProvider> rolloverSizeProvider = new AtomicReference<RolloverSizeProvider>();
    final AtomicReference<NumberGenerationProvider> numberGenerationProvider = new AtomicReference<NumberGenerationProvider>();

    public RollingFileWriter(String filename, RolloverSizeProvider rolloverSizeProvider, NumberGenerationProvider numberGenerationProvider) throws IOException {
        this(filename, rolloverSizeProvider);
        this.numberGenerationProvider.set(numberGenerationProvider);
    }

    public RollingFileWriter(String filename, RolloverSizeProvider rolloverSizeProvider) throws IOException {
        this.filename.set(filename);
        this.rolloverSizeProvider.set(rolloverSizeProvider);
        file.set(new File(filename));
        directory.set(file.get().getParent());
        if (file.get().exists())
            length.set(file.get().length());
        writer.set(new FileWriter(filename, true));
    }


    private void checkGenerations() throws IOException {
        int ngen = numberGenerationProvider.get().getNumberGenerations();
        if (ngen <= 0)
            return;
        File dir = new File(directory.get());
        if (!dir.exists()) {
            dir.mkdir();
            return;
        }
        final String fn = file.get().getName();
        String[] names = dir.list(new FilenameFilter() {
            public boolean accept(File file, String name) {
                return name.startsWith(fn) && name.endsWith(".old");
            }
        });
        if (names != null) {
            Arrays.sort(names, new Comparator<String>() {
                public int compare(String o1, String o2) {
                    return o1.substring(o1.indexOf(".old") - 14).compareTo(o2.substring(o2.indexOf(".old") - 14));
                }
            });
            int todDel = names.length - ngen;
            if (todDel > 0) {
                for (int i = 0; i < todDel; i++) {
                    new File(dir, names[i]).delete();
                }
            }
        }
    }

    private void checkRolling() throws IOException {
        long max = rolloverSizeProvider.get().getRollOverSize();
        if (max == -1)
            return;
        if (length.get() > max) {
            writer.get().flush();
            writer.get().close();
            File f = new File(filename + "-" + (generation.getAndIncrement()) + FMTROTATE.format(new Date()));
            file.get().renameTo(f);
            file.set(new File(filename.get()));
            writer.set(new FileWriter(filename.get(), true));
            length.set(0);
            if (numberGenerationProvider.get() != null)
                checkGenerations();
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