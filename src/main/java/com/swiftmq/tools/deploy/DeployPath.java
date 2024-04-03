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

package com.swiftmq.tools.deploy;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DeployPath {
    static final String CONFIG = "config.xml";
    static final String JAR = ".jar";
    static final String INTERNAL_DEPLOY_DIR = "_deployed_";
    static final String DELETED = ".deleted";
    File path = null;
    ClassLoader parentCL = null;
    ExtendableClassLoader classLoader = null;
    Map<String, Bundle> bundles = new ConcurrentHashMap<>();
    long startTime = System.currentTimeMillis();
    ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public DeployPath(File path, ClassLoader parentCL) {
        this.path = path;
        this.parentCL = parentCL;
    }

    public DeployPath(File path, boolean singleLoader, ClassLoader parentCL) {
        this.path = path;
        this.parentCL = parentCL;
        if (singleLoader) {
            try {
                classLoader = new ExtendableClassLoader(path, new URL[]{path.toURI().toURL()}, parentCL);
            } catch (Exception e) {
                e.printStackTrace();
            }
            collectJars();
        }
    }

    private void collectJars() {
        File[] files = path.listFiles();
        if (files != null) {
            for (File file : files) {
                if (!file.isDirectory() && file.getName().endsWith(JAR)) {
                    try {
                        classLoader.add(file.toURI().toURL());
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    public void init() {
        bundles.clear();
        startTime = System.currentTimeMillis();
    }

    public File getPath() {
        return (path);
    }

    private String loadConfigFile(File f) throws Exception {
        StringWriter writer = new StringWriter((int) f.length());
        BufferedReader reader = new BufferedReader(new FileReader(f));
        String line = null;
        while ((line = reader.readLine()) != null) {
            writer.write(line + "\n");
        }
        reader.close();
        writer.flush();
        return writer.toString();
    }

    private void checkConfigFile(Bundle bundle, File f) throws Exception {
        if (bundle.getChangeTimeConfig() != f.lastModified() || bundle.getBundleConfig() == null) {
            bundle.setBundleConfig(loadConfigFile(f), f.lastModified());
            // Don't overwrite previous detected events
            if (bundle.getEvent() == BundleEvent.BUNDLE_UNCHANGED)
                bundle.setEvent(BundleEvent.BUNDLE_CHANGED);
        }
    }

    // The bundle consists of every jar file within the bundle dir.
    // Check every jar file if it has been changed.
    // There must be at least 1 jar file to generate a BUNDLE_ADDED event.
    private void checkBundleJars(Bundle bundle, List jarFiles) throws Exception {
        List<BundleJar> bundleJars = null;
        if (jarFiles != null) {
            bundleJars = new ArrayList();
            List<URL> urls = new ArrayList();
            for (Object jarFile : jarFiles) {
                File f = (File) jarFile;
                urls.add(f.toURI().toURL());
                BundleJar bundleJar = new BundleJar(f.getName(), f.lastModified());
                bundleJars.add(bundleJar);
            }
            if (classLoader != null) {
                classLoader.add((URL[]) urls.toArray(new URL[urls.size()]));
                bundle.setBundleLoader(classLoader);
            }
            if (bundle.getEvent() == BundleEvent.BUNDLE_UNCHANGED) {
                List<BundleJar> oldBundleJars = bundle.getBundleJars();
                if (oldBundleJars == null)
                    bundle.setEvent(BundleEvent.BUNDLE_ADDED);
                else {
                    int event = BundleEvent.BUNDLE_UNCHANGED;
                    // check if there are new/changed bundle jars
                    for (Iterator<BundleJar> iter = bundleJars.iterator(); iter.hasNext() && event == BundleEvent.BUNDLE_UNCHANGED; ) {
                        BundleJar newBundleJar = iter.next();
                        boolean found = false;
                        for (Iterator<BundleJar> iter2 = oldBundleJars.iterator(); iter2.hasNext() && event == BundleEvent.BUNDLE_UNCHANGED && !found; ) {
                            BundleJar oldBundleJar = iter2.next();
                            if (newBundleJar.getFilename().equals(oldBundleJar.getFilename())) {
                                found = true;
                                if (newBundleJar.getLastModified() != oldBundleJar.getLastModified())
                                    event = BundleEvent.BUNDLE_CHANGED;
                            }
                        }
                        if (!found)
                            event = BundleEvent.BUNDLE_CHANGED;
                    }
                    if (event == BundleEvent.BUNDLE_UNCHANGED) {
                        // check if there are deleted bundle jars
                        for (Iterator<BundleJar> iter = oldBundleJars.iterator(); iter.hasNext() && event == BundleEvent.BUNDLE_UNCHANGED; ) {
                            BundleJar oldBundleJar = iter.next();
                            boolean found = false;
                            for (Iterator<BundleJar> iter2 = bundleJars.iterator(); iter2.hasNext() && event == BundleEvent.BUNDLE_UNCHANGED && !found; ) {
                                BundleJar newBundleJar = iter2.next();
                                if (newBundleJar.getFilename().equals(oldBundleJar.getFilename())) {
                                    found = true;
                                }
                            }
                            if (!found)
                                event = BundleEvent.BUNDLE_CHANGED;
                        }
                    }
                    bundle.setEvent(event);
                }
            }
        }
        bundle.setBundleJars(bundleJars);
    }

    private void checkDirectory(File dir) throws Exception {
        Bundle bundle = bundles.get(dir.getName());
        if (bundle == null) {
            bundle = new Bundle(dir.getName());
            bundle.setEvent(BundleEvent.BUNDLE_ADDED);
            bundles.put(bundle.getBundleName(), bundle);
        }
        File[] flist = dir.listFiles();
        List<File> jarFiles = null;
        if (flist != null) {
            for (File file : flist) {
                if (!file.isDirectory()) {
                    String fn = file.getName();
                    if (fn.equals(CONFIG))
                        checkConfigFile(bundle, file);
                    else if (fn.endsWith(JAR)) {
                        if (jarFiles == null)
                            jarFiles = new ArrayList<>();
                        jarFiles.add(file);
                    }
                }
            }
        }
        checkBundleJars(bundle, jarFiles);
    }

    // A bundle is removed if either the bundle dir has been removed
    // or the config.xml has been removed or if there is no more jar
    // file in the bundle dir.
    private void checkRemovedBundles() throws Exception {
        for (Map.Entry<String, Bundle> stringBundleEntry : bundles.entrySet()) {
            Bundle bundle = (Bundle) ((Map.Entry<?, ?>) stringBundleEntry).getValue();
            if (bundle.getEvent() == BundleEvent.BUNDLE_UNCHANGED) {
                String fn = path.getPath() + File.separatorChar + bundle.getBundleName();
                // check if bundle dir has been removed
                File f = new File(fn);
                if (!f.exists())
                    bundle.setEvent(BundleEvent.BUNDLE_REMOVED);
                else {
                    // check if bundle config has been removed
                    File configFile = new File(f, CONFIG);
                    if (!configFile.exists())
                        bundle.setEvent(BundleEvent.BUNDLE_REMOVED);
                    else {
                        // check if all bundle jars have been removed
                        if (bundle.getBundleJars() == null)
                            bundle.setEvent(BundleEvent.BUNDLE_REMOVED);
                    }
                }
            }
        }
    }

    private void removeDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory())
                    removeDirectory(file);
                file.delete();
            }
        }
        dir.delete();
    }

    private URL copyFile(File f, String dir) throws Exception {
        File dest = new File(dir + File.separatorChar + f.getName());
        BufferedInputStream fis = new BufferedInputStream(new FileInputStream(f), 16384);
        BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(dest), 16384);
        byte b[] = new byte[16384];
        int len = 0;
        while ((len = fis.read(b)) != -1)
            fos.write(b, 0, len);
        fos.flush();
        fis.close();
        fos.close();
        dest.setLastModified(f.lastModified());
        return dest.toURI().toURL();
    }

    private void createInternalDeployment(Bundle bundle) throws Exception {
        String bundleDirName = path.getPath() + File.separatorChar + bundle.getBundleName();
        String intDeployDirName = bundleDirName + File.separatorChar + INTERNAL_DEPLOY_DIR + System.currentTimeMillis();
        bundle.setBundleDir(intDeployDirName);
        File bundleDir = new File(bundleDirName);
        File intDeployDir = new File(intDeployDirName);
        intDeployDir.mkdir();
        File[] sources = bundleDir.listFiles();
        if (sources != null) {
            List<URL> urls = new ArrayList();
            for (File source : sources) {
                if (source.getName().equals(CONFIG))
                    copyFile(source, intDeployDirName);
                else if (source.getName().endsWith(JAR))
                    urls.add(copyFile(source, intDeployDirName));
                else if (!source.isDirectory())
                    copyFile(source, intDeployDirName);
            }
            bundle.setBundleLoader(new ExtendableClassLoader(intDeployDir, (URL[]) urls.toArray(new URL[urls.size()]), parentCL));
        }
    }

    private boolean isDeleted(File dir) {
        File[] files = dir.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return file.getName().equals(DELETED);
            }
        });
        return files != null && files.length > 0;
    }

    private void purgeBundleDir(File dir) {
        File[] files = dir.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return file.isDirectory() && file.getName().startsWith(INTERNAL_DEPLOY_DIR) && isDeleted(file);
            }
        });
        if (files != null) {
            for (File file : files) {
                removeDirectory(file);
            }
        }
    }

    public void purge() {
        lock.writeLock().lock();
        try {
            File[] files = path.listFiles(new FileFilter() {
                public boolean accept(File file) {
                    return file.isDirectory();
                }
            });
            if (files != null) {
                for (File file : files) {
                    purgeBundleDir(file);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }

    }

    private Bundle getInstalledBundle(File dir) throws Exception {
        File[] files = dir.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return file.isDirectory() && !isDeleted(file);
            }
        });
        if (files == null || files.length == 0)
            return null;
        Bundle bundle = new Bundle(dir.getName());
        bundle.setBundleDir(files[0].getPath());
        File[] sources = files[0].listFiles();
        if (sources != null) {
            List<BundleJar> bundleJars = new ArrayList();
            List<URL> urls = new ArrayList();
            for (File source : sources) {
                if (source.getName().equals(CONFIG))
                    bundle.setBundleConfig(loadConfigFile(source), source.lastModified());
                else if (source.getName().endsWith(JAR)) {
                    urls.add(source.toURI().toURL());
                    bundleJars.add(new BundleJar(source.getName(), source.lastModified()));
                }
            }
            bundle.setBundleJars(bundleJars);
            bundle.setBundleLoader(new ExtendableClassLoader(dir, (URL[]) urls.toArray(new URL[urls.size()]), parentCL));
        }
        bundles.put(bundle.getBundleName(), bundle);
        return bundle;
    }

    public List<Bundle> getInstalledBundles() throws Exception {
        lock.readLock().lock();
        try {
            List<Bundle> list = null;
            File[] files = path.listFiles(new FileFilter() {
                public boolean accept(File file) {
                    return file.isDirectory();
                }
            });
            if (files != null) {
                for (File file : files) {
                    Bundle b = getInstalledBundle(file);
                    if (b != null) {
                        if (list == null)
                            list = new ArrayList<>();
                        list.add(b);
                    }
                }
            }
            return list;
        } finally {
            lock.readLock().unlock();
        }

    }

    private void markDeleted(Bundle bundle) {
        String bundleDir = bundle.getBundleDir();
        if (bundleDir == null)
            return;
        File f = new File(bundle.getBundleDir() + File.separatorChar + DELETED);
        try {
            f.createNewFile();
        } catch (Exception ignored) {
        }
    }

    public void removeBundle(Bundle bundle) {
        lock.writeLock().lock();
        try {
            bundles.remove(bundle.getBundleName());
            markDeleted(bundle);
        } finally {
            lock.writeLock().unlock();
        }

    }

    public BundleEvent[] getBundleEvents() throws Exception {
        lock.writeLock().lock();
        try {
            // check all file for changes and record the changes in the bundle
            File[] flist = path.listFiles();
            if (flist != null) {
                for (File file : flist) {
                    if (file.isDirectory()) {
                        checkDirectory(file);
                    }
                }
            }
            // check for removed bundle and record the changes in the bundle
            checkRemovedBundles();

            // Flip through the bundles and check for event BUNDLE_REMOVED
            // to ensure that those events are delivered before additions.
            List<BundleEvent> al = null;
            for (Iterator<Map.Entry<String, Bundle>> iter = bundles.entrySet().iterator(); iter.hasNext(); ) {
                Bundle bundle = (Bundle) ((Map.Entry<?, ?>) iter.next()).getValue();
                if (bundle.getEvent() == BundleEvent.BUNDLE_REMOVED) {
                    if (al == null)
                        al = new ArrayList();
                    BundleEvent bundleEvent = new BundleEvent(bundle.getEvent(), bundle);
                    // Delete removed bundles
                    iter.remove();
                    markDeleted(bundle);
                    al.add(bundleEvent);
                }
            }

            // flip through the bundles and check BUNDLE_ADDED/CHANGED
            // and if both, config file and bundle jars, are set.
            // Create internal deployment and generate an event.
            for (Map.Entry<String, Bundle> stringBundleEntry : bundles.entrySet()) {
                Bundle bundle = (Bundle) ((Map.Entry<?, ?>) stringBundleEntry).getValue();
                if ((bundle.getEvent() == BundleEvent.BUNDLE_ADDED ||
                        bundle.getEvent() == BundleEvent.BUNDLE_CHANGED) &&
                        bundle.getBundleConfig() != null &&
                        bundle.getBundleJars() != null) {
                    if (classLoader == null) {
                        if (bundle.getEvent() == BundleEvent.BUNDLE_CHANGED)
                            markDeleted(bundle);
                        createInternalDeployment(bundle);
                    }
                    if (al == null)
                        al = new ArrayList();
                    BundleEvent bundleEvent = new BundleEvent(bundle.getEvent(), bundle);
                    al.add(bundleEvent);
                    bundle.setEvent(BundleEvent.BUNDLE_UNCHANGED);
                }
            }
            if (al == null || al.size() == 0)
                return null;
            return al.toArray(new BundleEvent[al.size()]);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String toString() {
        return "[DeployPath, path=" + path + ", bundles=" + bundles + "]";
    }
}

