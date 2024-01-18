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

package com.swiftmq.swiftlet;

import com.swiftmq.client.thread.PoolManager;
import com.swiftmq.mgmt.*;
import com.swiftmq.swiftlet.event.KernelStartupListener;
import com.swiftmq.swiftlet.event.SwiftletManagerEvent;
import com.swiftmq.swiftlet.event.SwiftletManagerListener;
import com.swiftmq.swiftlet.log.LogSwiftlet;
import com.swiftmq.swiftlet.timer.TimerSwiftlet;
import com.swiftmq.swiftlet.trace.TraceSpace;
import com.swiftmq.swiftlet.trace.TraceSwiftlet;
import com.swiftmq.tools.deploy.Bundle;
import com.swiftmq.tools.deploy.BundleEvent;
import com.swiftmq.tools.deploy.DeployPath;
import com.swiftmq.tools.file.NumberBackupFileReducer;
import com.swiftmq.tools.log.NullPrintStream;
import com.swiftmq.upgrade.UpgradeUtilities;
import com.swiftmq.util.SwiftUtilities;
import com.swiftmq.util.Version;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Security;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * The SwiftletManager is the single instance of a SwiftMQ router that is
 * acting as a container for Swiftlets. It is responsible for:
 * <br>
 * <ul>
 * <li>startup/shutdown of the router (halt/reboot)</li>
 * <li>startup/shutdown of Kernel Swiftlets</li>
 * <li>startup/shutdown of Extension Swiftlets</li>
 * <li>load/save of the configuration file</li>
 * </ul>
 * During the startup phase of the router, the SwiftletManager loads
 * the router's configuration file, and starts all Kernel Swiftlets.
 * The startup order is defined in the 'startorder' attribute of
 * the 'router' tag element of the configuration file.
 * <br>
 * For each Swiftlet defined in the startorder attribute, the following
 * tasks are performed:
 * <br>
 * <ul>
 * <li>Instantiating the Swiftlet class from the kernel classloader</li>
 * <li>Invoking the <code>startup</code> method of the Swiftlet. The
 * Swiftlet configuration is passed as parameter.</li>
 * </ul>
 * The router is running after all Kernel Swiftlet's <code>startup</code>
 * methods have been processed without exceptions.
 * <br>
 * <br>
 * A shutdown of a router is processed in the reverse order and the
 * <code>shutdown</code> method of each Swiftlet is invoked.
 * <br>
 * <br>
 * Extension Swiftlets are loaded/unloaded on request through the methods
 * <code>loadExtensionSwiftlet</code> and <code>unloadExtensionSwiftlet</code>.
 *
 * @author IIT GmbH, Bremen/Germany, Copyright (c) 2000-2002, All Rights Reserved
 */
public class SwiftletManager {
    static final String PROP_INITIAL_CONFIG = "swiftmq.initialconfig";
    static final String PROP_PRECONFIG = "swiftmq.preconfig";
    static final String PROP_SHUTDOWN_HOOK = "swiftmq.shutdown.hook";
    static final String PROP_REUSE_KERNEL_CL = "swiftmq.reuse.kernel.classloader";
    static final long PROP_CONFIG_WATCHDOG_INTERVAL = Long.parseLong(System.getProperty("swiftmq.config.watchdog.interval", "0"));
    protected static final AtomicReference<SwiftletManager> _instance = new AtomicReference<>();
    static SimpleDateFormat fmt = new SimpleDateFormat(".yyyyMMddHHmmssSSS");
    final AtomicReference<String> configFilename = new AtomicReference<>();
    final AtomicReference<Document> routerConfig = new AtomicReference<>();
    final AtomicReference<String> routerName = new AtomicReference<>();
    final AtomicReference<String> workingDirectory = new AtomicReference<>(System.getProperty("user.dir"));
    String[] kernelSwiftletNames = null;
    Map<String, Swiftlet> swiftletTable = null;
    DeployPath dp = null;
    Map<String, Bundle> bundleTable = null;
    Map<String, HashSet<SwiftletManagerListener>> listeners = new ConcurrentHashMap<>();
    Set allListeners = ConcurrentHashMap.newKeySet();
    Set<KernelStartupListener> kernelListeners = new HashSet<>();
    Map<String, Object> surviveMap = new ConcurrentHashMap<String, Object>();
    RouterMemoryMeter memoryMeter = null;
    SwiftletDeployer swiftletDeployer = null;

    LogSwiftlet logSwiftlet = null;
    TraceSwiftlet traceSwiftlet = null;
    TimerSwiftlet timerSwiftlet = null;

    ConfigfileWatchdog configfileWatchdog = null;

    TraceSpace traceSpace = null;
    final AtomicLong memCollectInterval = new AtomicLong(10000);
    final AtomicBoolean smartTree = new AtomicBoolean(true);
    final AtomicBoolean startup = new AtomicBoolean(false);
    final AtomicBoolean rebooting = new AtomicBoolean(false);
    final AtomicBoolean workingDirAdded = new AtomicBoolean(false);
    final AtomicBoolean registerShutdownHook = new AtomicBoolean(Boolean.parseBoolean(System.getProperty(PROP_SHUTDOWN_HOOK, "true")));
    final AtomicBoolean quietMode = new AtomicBoolean(false);
    final AtomicBoolean strippedMode = new AtomicBoolean(false);
    final AtomicBoolean doFireKernelStartedEvent = new AtomicBoolean(true);
    final AtomicBoolean configDirty = new AtomicBoolean(false);
    PrintStream savedSystemOut = System.out;
    Thread shutdownHook = null;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    protected SwiftletManager() {
    }

    /**
     * Returns the singleton instance of the SwiftletManager
     *
     * @return singleton instance
     */
    public static SwiftletManager getInstance() {
        if (_instance.get() == null) {
            _instance.compareAndSet(null, new SwiftletManager());
        }
        return _instance.get();
    }

    public boolean isHA() {
        return false;
    }

    public void setDoFireKernelStartedEvent(boolean doFireKernelStartedEvent) {
        this.doFireKernelStartedEvent.set(doFireKernelStartedEvent);
    }

    protected void trace(String message) {
        if (!quietMode.get() && traceSpace != null && traceSpace.enabled)
            traceSpace.trace("SwiftletManager", message);
    }

    protected void startSwiftletDeployer() {
        swiftletDeployer = new SwiftletDeployer();
        swiftletDeployer.start();
    }

    protected void stopSwiftletDeployer() {
        if (swiftletDeployer != null)
            swiftletDeployer.stop();
    }

    protected Configuration getConfiguration(Swiftlet swiftlet) throws Exception {
        trace("Swiftlet " + swiftlet.getName() + "', getConfiguration");
        Configuration config = (Configuration) RouterConfiguration.Singleton().getConfigurations().get(swiftlet.getName());
        if (config == null) {
            trace("Swiftlet " + swiftlet.getName() + "', get configuration template");
            config = getConfigurationTemplate(swiftlet.getName());
            if (config == null)
                throw new Exception("Swiftlet " + swiftlet.getName() + "', getConfigurationTemplate returns null");
            trace("Swiftlet " + swiftlet.getName() + "', fill configuration");
            config.getMetaData().setName(swiftlet.getName());
            config = fillConfiguration(config);
        }
        return config;
    }

    /**
     * Returns the configuration of a specific Swiftlet.
     *
     * @param name Swiftlet name, e.g. "sys$topicmanager".
     * @return configuration
     */
    public Configuration getConfiguration(String name) {
        return (Configuration) RouterConfiguration.Singleton().getConfigurations().get(name);
    }

    private void startUpSwiftlet(Swiftlet swiftlet, Configuration config) throws SwiftletException {
        System.out.println("... startup: " + config.getMetaData().getDisplayName());
        if (logSwiftlet != null)
            logSwiftlet.logInformation("SwiftletManager", "Swiftlet starting: " + swiftlet.getName() + " ...");
        if (swiftlet.isKernel()) {
            trace("Swiftlet " + swiftlet.getName() + "', fireSwiftletManagerEvent: swiftletStartInitiated");
            fireSwiftletManagerEvent(swiftlet.getName(), "swiftletStartInitiated", new SwiftletManagerEvent(this, swiftlet.getName()));
            trace("Swiftlet " + swiftlet.getName() + "', swiftlet.startup()");
        }
        swiftlet.startup(config);
        swiftlet.setState(Swiftlet.STATE_ACTIVE);
        if (logSwiftlet != null)
            logSwiftlet.logInformation("SwiftletManager", "Swiftlet started: " + swiftlet.getName());
        if (swiftlet.isKernel()) {
            trace("Swiftlet " + swiftlet.getName() + "', fireSwiftletManagerEvent: swiftletStarted");
            fireSwiftletManagerEvent(swiftlet.getName(), "swiftletStarted", new SwiftletManagerEvent(this, swiftlet.getName()));
        }
    }

    protected void shutdownSwiftlet(Swiftlet swiftlet) throws SwiftletException {
        try {
            Configuration config = getConfiguration(swiftlet);
            System.out.println("... shutdown: " + config.getMetaData().getDisplayName());
        } catch (Exception ignored) {
        }
        SwiftletShutdown ss = new SwiftletShutdown(swiftlet);
        Thread t = new Thread(ss);
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
        }
        if (ss.getException() != null)
            throw ss.getException();
    }

    protected void startKernelSwiftlets() {
        String actSwiftletName = null;
        Swiftlet swiftlet = null;
        long startupTime = -1;
        String className = null;

        try {
            // First: start TraceSwiftlet
            actSwiftletName = "sys$trace";
            swiftlet = null;
            startupTime = -1;
            try {
                swiftlet = (TraceSwiftlet) loadSwiftlet(actSwiftletName);
            } catch (Exception e) {
                System.err.println("Exception occurred while creating TraceSwiftlet instance: " + e.getMessage());
                System.exit(-1);
            }
            swiftlet.setName(actSwiftletName);
            swiftlet.setKernel(true);
            Configuration c = getConfiguration(swiftlet);
            RouterConfiguration.Singleton().getConfigurations().put(actSwiftletName, c);
            startUpSwiftlet(swiftlet, c);
            swiftlet.setStartupTime(System.currentTimeMillis());
            swiftletTable.put(actSwiftletName, swiftlet);
            traceSwiftlet = (TraceSwiftlet) swiftlet;
            traceSpace = traceSwiftlet.getTraceSpace(TraceSwiftlet.SPACE_KERNEL);
            trace("Trace swiftlet '" + actSwiftletName + " has been started");
            trace("Starting kernel swiftlets");

            // Next: start the other kernel swiftlets
            for (String kernelSwiftletName : kernelSwiftletNames) {
                actSwiftletName = kernelSwiftletName;
                startKernelSwiftlet(actSwiftletName, swiftletTable);
                if (kernelSwiftletName.equals("sys$log"))
                    logSwiftlet = (LogSwiftlet) getSwiftlet("sys$log");
            }

            // Create Extension Swiftlet Deployer
            if (!isHA())
                startSwiftletDeployer();
            saveConfigIfDirty();
        } catch (Exception e) {
            e.printStackTrace();
            trace("Kernel swiftlet: '" + actSwiftletName + "', exception during startup: " + e.getMessage());
            System.err.println("Exception during startup kernel swiftlet '" + actSwiftletName + "': " + e.getMessage());
            System.exit(-1);
        }
        trace("Kernel swiftlets started");
    }

    protected void startKernelSwiftlet(String actSwiftletName, Map<String, Swiftlet> table) throws Exception {
        Swiftlet swiftlet;
        long startupTime;
        trace("Starting kernel swiftlet: '" + actSwiftletName + "' ...");
        swiftlet = null;
        startupTime = -1;
        trace("Kernel swiftlet: '" + actSwiftletName + "'");
        try {
            swiftlet = (Swiftlet) loadSwiftlet(actSwiftletName);
        } catch (Exception e) {
            e.printStackTrace();
            trace("Kernel swiftlet: '" + actSwiftletName + "', exception occurred while creating Swiftlet instance: " + e.getMessage());
            System.err.println("Exception occurred while creating Swiftlet instance: " + e.getMessage());
            System.exit(-1);
        }
        swiftlet.setName(actSwiftletName);
        swiftlet.setKernel(true);
        trace("Kernel swiftlet: '" + actSwiftletName + "', startUpSwiftlet ...");
        table.put(actSwiftletName, swiftlet);
        Configuration conf = getConfiguration(swiftlet);
        RouterConfiguration.Singleton().getConfigurations().put(actSwiftletName, conf);
        startUpSwiftlet(swiftlet, conf);
        swiftlet.setStartupTime(System.currentTimeMillis());
        trace("Kernel swiftlet: '" + actSwiftletName + "', is running");
    }

    protected void stopKernelSwiftlets() {
        lock.writeLock().lock();
        try {
            trace("stopKernelSwiftlets");
            logSwiftlet.logInformation("SwiftletManager", "stopKernelSwiftlets");
            stopSwiftletDeployer();
            List<Swiftlet> al = new ArrayList<Swiftlet>();
            for (int i = kernelSwiftletNames.length - 1; i >= 0; i--) {
                String name = kernelSwiftletNames[i];
                Swiftlet swiftlet = swiftletTable.get(name);
                if (swiftlet.getState() == Swiftlet.STATE_ACTIVE) {
                    al.add(swiftlet);
                }
            }
            al.add(swiftletTable.get("sys$trace"));
            al.forEach(swiftlet -> {
                try {
                    shutdownSwiftlet(swiftlet);
                } catch (SwiftletException ignored) {
                }
                swiftlet.setStartupTime(-1);
            });
        } finally {
            lock.writeLock().unlock();
        }

    }

    private void fillSwiftletTable() {
        for (String kernelSwiftletName : kernelSwiftletNames) swiftletTable.put(kernelSwiftletName, null);
    }

    public String getWorkingDirectory() {
        return workingDirectory.get();
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory.set(workingDirectory);
    }

    public boolean isRegisterShutdownHook() {
        return registerShutdownHook.get();
    }

    public void setRegisterShutdownHook(boolean registerShutdownHook) {
        this.registerShutdownHook.set(registerShutdownHook);
    }

    public void disableShutdownHook() {
        if (shutdownHook != null) {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
            shutdownHook = null;
        }
    }

    public boolean isQuietMode() {
        return quietMode.get();
    }

    public void setQuietMode(boolean quietMode) {
        this.quietMode.set(quietMode);
        if (quietMode)
            System.setOut(new NullPrintStream());
        else
            System.setOut(savedSystemOut);
    }

    public boolean isStrippedMode() {
        return strippedMode.get();
    }

    public void setStrippedMode(boolean strippedMode) {
        this.strippedMode.set(strippedMode);
    }

    public void setConfigDirty(boolean configDirty) {
        this.configDirty.set(configDirty);
    }

    public void saveConfigIfDirty() {
        if (configDirty.get()) {
            logSwiftlet.logInformation("SwiftletManager", "Configuration was updated, saving ...");
            saveConfiguration();
        }
    }

    public String getLastSwiftlet() {
        return kernelSwiftletNames[kernelSwiftletNames.length - 1];
    }

    /**
     * Loads a new Extension Swiftlet. Will be used from the Deploy Swiftlet only.
     *
     * @param bundle deployment bundle.
     * @throws Exception on error during load
     */
    public void loadExtensionSwiftlet(Bundle bundle) throws Exception {
        lock.writeLock().lock();
        try {
            String name = bundle.getBundleName();
            trace("loadExtensionSwiftlet: '" + name + "' ...");
            bundleTable.put(bundle.getBundleName(), bundle);
            Swiftlet swiftlet = loadSwiftlet(name);
            swiftlet.setName(name);
            long startupTime = -1;

            Configuration config = getConfiguration(swiftlet);
            RouterConfiguration.Singleton().addEntity(config);
            config.setExtension(true);

            trace("Swiftlet: '" + name + "', startUpSwiftlet ...");
            startUpSwiftlet(swiftlet, config);
            startupTime = System.currentTimeMillis();
            swiftlet.setStartupTime(startupTime);
            swiftletTable.put(name, swiftlet);
            swiftlet = null;
            saveConfiguration();
            trace("loadExtensionSwiftlet: '" + name + "' DONE.");
        } finally {
            lock.writeLock().unlock();
        }

    }

    /**
     * Unloads an Extension Swiftlet. Will be used from the Deploy Swiftlet only.
     *
     * @param bundle deployment bundle.
     */
    public void unloadExtensionSwiftlet(Bundle bundle) {
        lock.writeLock().lock();
        try {
            String name = bundle.getBundleName();
            trace("unloadExtensionSwiftlet: '" + name + "' ...");
            try {
                Swiftlet swiftlet = swiftletTable.get(name);
                if (swiftlet != null)
                    shutdownSwiftlet(swiftlet);
                RouterConfiguration.Singleton().removeEntity(RouterConfiguration.Singleton().getEntity(name));
            } catch (Exception ignored) {
            }
            bundleTable.remove(name);
            swiftletTable.remove(name);
            System.gc();
            System.runFinalization();
            trace("unloadExtensionSwiftlet: '" + name + "' DONE.");
        } finally {
            lock.writeLock().unlock();
        }

    }

    /**
     * Returns true if the router is configured to use a smart management tree to avoid
     * overloading management tools like SwiftMQ Explorer or CLI with management messages
     * when connected to a router running a high load. This is only a hint. Each Swiftlet
     * is responsible which content it puts into the management tree, especially the usage
     * part.
     *
     * @return true/false.
     */
    public boolean isUseSmartTree() {
        return smartTree.get();
    }

    /**
     * Returns true if the router is within the startup phase.
     *
     * @return true/false.
     */
    public boolean isStartup() {
        return startup.get();
    }

    /**
     * Returns true if the router is within the reboot phase.
     *
     * @return true/false.
     */
    public boolean isRebooting() {
        return rebooting.get();
    }

    protected Map<String, Bundle> createBundleTable(String kernelPath) throws Exception {
        if (kernelPath == null)
            throw new Exception("Missing attribute: kernelpath");
        File f = new File(SwiftUtilities.addWorkingDir(kernelPath));
        if (!f.exists() || !f.isDirectory())
            throw new Exception("Invalid value for 'kernelpath': directory doesn't exists");
        if (dp == null)
            dp = new DeployPath(f, true, getClass().getClassLoader());
        else {
            boolean reuseCL = Boolean.valueOf(System.getProperty(PROP_REUSE_KERNEL_CL, "false"));
            if (reuseCL)
                dp.init();
            else
                dp = new DeployPath(f, true, getClass().getClassLoader());
        }
        BundleEvent[] events = dp.getBundleEvents();
        if (events == null)
            throw new Exception("No Kernel Swiftlets found in 'kernelpath'");
        Map<String, Bundle> table = new HashMap<String, Bundle>();
        for (BundleEvent event : events) {
            Bundle b = event.getBundle();
            table.put(b.getBundleName(), b);
        }
        return table;
    }

    private Document getInitialConfig() throws Exception {
        String initialConfig = System.getProperty(PROP_INITIAL_CONFIG);
        if (initialConfig != null && initialConfig.trim().length() > 0)
            return XMLUtilities.createDocument(new FileInputStream(initialConfig));
        return routerConfig.get();
    }

    /**
     * Checks and applies a preconfiguration.
     *
     * @throws Exception
     */
    private void checkAndApplyPreconfig() throws Exception {
        String preconfig = System.getProperty(PROP_PRECONFIG);
        if (preconfig != null && preconfig.trim().length() > 0) {
            StringTokenizer t = new StringTokenizer(preconfig, ",");
            while (t.hasMoreTokens()) {
                String pc = t.nextToken();
                XMLUtilities.writeDocument(routerConfig.get(), configFilename + fmt.format(new Date()));
                routerConfig.set(new PreConfigurator(routerConfig.get(), XMLUtilities.createDocument(new FileInputStream(pc))).applyChanges());
                XMLUtilities.writeDocument(routerConfig.get(), configFilename.get());
                System.out.println("Applied changes from preconfig file: " + pc);
            }
        }
    }

    /**
     * Starts the router.
     * This method is called from the bootstrap class only.
     *
     * @param name name of the configuration file.
     * @throws Exception on error.
     */
    public void startRouter(String name) throws Exception {
        if (!workingDirAdded.get()) {
            configFilename.set(SwiftUtilities.addWorkingDir(name));
            workingDirAdded.set(true);
        }
        routerConfig.set(XMLUtilities.createDocument(new FileInputStream(configFilename.get())));

        UpgradeUtilities.checkRelease(configFilename.get(), routerConfig.get());
        checkAndApplyPreconfig();
        Element root = routerConfig.get().getRootElement();
        parseOptionalConfiguration(root);
        String value = root.attributeValue("startorder");
        if (value == null)
            throw new Exception("Missing attribute: startorder");
        StringTokenizer t = new StringTokenizer(value, " ,:");
        kernelSwiftletNames = new String[t.countTokens()];
        int i = 0;
        while (t.hasMoreTokens())
            kernelSwiftletNames[i++] = t.nextToken();
        if (root.attributeValue("use-smart-tree") != null)
            smartTree.set(Boolean.parseBoolean(root.attributeValue("use-smart-tree")));
        else
            smartTree.set(true);
        if (root.attributeValue("memory-collect-interval") != null)
            memCollectInterval.set(Long.valueOf(root.attributeValue("memory-collect-interval")));
        routerName.set(root.attributeValue("name"));

        bundleTable = createBundleTable(root.attributeValue("kernelpath"));

        initSwiftlets();

        timerSwiftlet = (TimerSwiftlet) getSwiftlet("sys$timer");

        if (PROP_CONFIG_WATCHDOG_INTERVAL > 0) {
            configfileWatchdog = new ConfigfileWatchdog(traceSpace, logSwiftlet, configFilename.get());
            timerSwiftlet.addTimerListener(PROP_CONFIG_WATCHDOG_INTERVAL, configfileWatchdog);
        }

        // shutdown hook
        if (shutdownHook == null && registerShutdownHook.get()) {
            shutdownHook = new Thread(() -> shutdown());
            Runtime.getRuntime().addShutdownHook(shutdownHook);
        }
    }

    protected void parseOptionalConfiguration(Element root) {
    }

    protected void createRouterCommands() {
        RouterConfiguration.Singleton().setName(getRouterName());
        RouterConfiguration.Singleton().createCommands(); // for compatibility
        CommandRegistry commandRegistry = new CommandRegistry("current Router's Swiftlet Manager", null);
        RouterConfiguration.Singleton().setCommandRegistry(commandRegistry);
        CommandExecutor rebootExecutor = (context, entity, cmd) -> {
            if (cmd.length != 1)
                return new String[]{TreeCommands.ERROR, "Invalid command, please try 'reboot'"};
            String[] result = new String[]{TreeCommands.INFO, "Reboot Launch in 10 Seconds."};
            Thread t = new Thread(Thread.currentThread().getThreadGroup().getParent(), "Reboot Thread") {
                public void run() {
                    reboot(10000);
                }
            };
            t.setDaemon(false); // Important: Must be a non-daemon, otherwise the shutdown hook is activated!
            t.setPriority(1); // IMPORTANT!!! MINIMUM PRIORITY IS REQUIRED
            t.start();
            return result;
        };
        Command rebootCommand = new Command("reboot", "reboot", "Reboot the Router", true, rebootExecutor, true, false);
        commandRegistry.addCommand(rebootCommand);
        CommandExecutor haltExecutor = (context, entity, cmd) -> {
            if (cmd.length != 1)
                return new String[]{TreeCommands.ERROR, "Invalid command, please try 'halt'"};
            String[] result = new String[]{TreeCommands.INFO, "Router Halt in 10 Seconds."};
            Thread t = new Thread(Thread.currentThread().getThreadGroup().getParent(), "Halt Thread") {
                public void run() {
                    try {
                        Thread.sleep(10000);
                    } catch (Exception ignored) {
                    }
                    shutdown(true);
                    System.exit(0);
                }
            };
            t.setDaemon(false); // Important: Must be a non-daemon, otherwise the shutdown hook is activated!
            t.start();
            t.setPriority(1); // IMPORTANT!!! MINIMUM PRIORITY IS REQUIRED
            return result;
        };
        Command haltCommand = new Command("halt", "halt", "Halt the Router", true, haltExecutor, true, false);
        commandRegistry.addCommand(haltCommand);
        CommandExecutor saveExecutor = (context, entity, cmd) -> {
            if (cmd.length > 1)
                return new String[]{TreeCommands.ERROR, "Invalid command, please try 'save'"};
            String[] result = null;
            result = saveConfiguration(RouterConfiguration.Singleton());
            return result;
        };
        Command saveCommand = new Command("save", "save", "Save this Router Configuration", true, saveExecutor, true, false);
        commandRegistry.addCommand(saveCommand);
    }

    private void initSwiftlets() {
        lock.writeLock().lock();
        try {
            System.out.println("Booting SwiftMQ " + Version.getKernelVersion() + " " + "[" + getRouterName() + "] ...");
            /*${evalprintout}*/
            startup.set(true);
            swiftletTable = new ConcurrentHashMapWithNulls<>();

            createRouterCommands();

            try {
                Entity envEntity = new Entity(Configuration.ENV_ENTITY,
                        "Router Environment",
                        "Environment of this Router",
                        null);
                envEntity.createCommands();
                RouterConfiguration.Singleton().addEntity(envEntity);

                Property prop = new Property("routername");
                prop.setType(String.class);
                prop.setDisplayName("Router Name");
                prop.setDescription("Name of this Router");
                prop.setValue(getRouterName());
                prop.setRebootRequired(true);
                prop.setPropertyChangeListener(new PropertyChangeAdapter(null) {
                    public void propertyChanged(Property property, Object oldValue, Object newValue)
                            throws PropertyChangeException {
                        try {
                            if (newValue != null)
                                SwiftUtilities.verifyRouterName((String) newValue);
                        } catch (Exception e) {
                            throw new PropertyChangeException(e.getMessage());
                        }
                    }
                });
                envEntity.addProperty(prop.getName(), prop);
                prop = new Property("use-smart-tree");
                prop.setType(Boolean.class);
                prop.setDisplayName("Use Smart Management Tree");
                prop.setDescription("Use Smart Management Tree (reduced Usage Parts)");
                prop.setValue(smartTree.get());
                prop.setRebootRequired(true);
                envEntity.addProperty(prop.getName(), prop);
                prop = new Property("release");
                prop.setType(String.class);
                prop.setDisplayName("SwiftMQ Release");
                prop.setDescription("SwiftMQ Release");
                prop.setValue(Version.getKernelVersion());
                prop.setReadOnly(true);
                prop.setStorable(false);
                envEntity.addProperty(prop.getName(), prop);
                prop = new Property("hostname");
                prop.setType(String.class);
                prop.setDisplayName("Hostname");
                prop.setDescription("Router's DNS Hostname");
                String localhost = "unknown";
                try {
                    localhost = InetAddress.getByName(InetAddress.getLocalHost().getHostAddress()).getHostName();
                } catch (UnknownHostException e) {
                    System.err.println("Unable to determine local host name: " + e);
                }
                prop.setValue(localhost);
                prop.setReadOnly(true);
                prop.setStorable(false);
                envEntity.addProperty(prop.getName(), prop);
                prop = new Property("startuptime");
                prop.setType(String.class);
                prop.setDisplayName("Startup Time");
                prop.setDescription("Router's Startup Time");
                prop.setValue(new Date().toString());
                prop.setReadOnly(true);
                prop.setStorable(false);
                envEntity.addProperty(prop.getName(), prop);
                prop = new Property("os");
                prop.setType(String.class);
                prop.setDisplayName("Operating System");
                prop.setDescription("Router's Host OS");
                prop.setValue(System.getProperty("os.name") + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch") + " ");
                prop.setReadOnly(true);
                prop.setStorable(false);
                envEntity.addProperty(prop.getName(), prop);
                prop = new Property("jre");
                prop.setType(String.class);
                prop.setDisplayName("JRE");
                prop.setDescription("JRE Version");
                prop.setValue(System.getProperty("java.version"));
                prop.setReadOnly(true);
                prop.setStorable(false);
                envEntity.addProperty(prop.getName(), prop);
                prop = new Property("memory-collect-interval");
                prop.setType(Long.class);
                prop.setDisplayName("Memory Collect Interval");
                prop.setDescription("Memory Collect Interval (ms)");
                prop.setValue(memCollectInterval.get());
                prop.setReadOnly(false);
                prop.setStorable(false);
                envEntity.addProperty(prop.getName(), prop);
                memoryMeter = new RouterMemoryMeter(prop);
                envEntity.addEntity(memoryMeter.getMemoryList());
            } catch (Exception e) {
                e.printStackTrace();
            }

            fillSwiftletTable();
            startKernelSwiftlets();
            trace("Init swiftlets successful");
            memoryMeter.start();
            startup.set(false);
            if (doFireKernelStartedEvent.get())
                fireKernelStartedEvent();
            logSwiftlet.logInformation("SwiftletManager", "networkaddress.cache.ttl=" + Security.getProperty("networkaddress.cache.ttl"));
            System.out.println("SwiftMQ " + Version.getKernelVersion() + " " + "[" + getRouterName() + "] is ready.");
        } finally {
            lock.writeLock().unlock();
        }

    }

    /**
     * Reboots this router without delay. A separate thread is used. The method returns immediately.
     */
    public void reboot() {
        new Thread(() -> reboot(0)).start();
    }

    /**
     * Reboots this router with delay. A separate thread is NOT used. The method return after the reboot is done.
     * This method call must be used from a separate thread.
     *
     * @param delay A reboot delay in ms
     */
    public void reboot(long delay) {
        if (rebooting.get())
            return;
        rebooting.set(true);
        try {
            Thread.sleep(delay);
        } catch (Exception ignored) {
        }
        shutdown();
        System.gc();
        try {
            Thread.sleep(5000);
        } catch (Exception ignored) {
        }
        try {
            startRouter(configFilename.get());
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
        rebooting.set(false);
    }

    protected Swiftlet loadSwiftlet(String swiftletName) throws Exception {
        Bundle bundle = bundleTable.get(swiftletName);
        if (bundle == null)
            throw new Exception("No bundle found for Swiftlet '" + swiftletName + "'");
        Document doc = XMLUtilities.createDocument(bundle.getBundleConfig());
        String className = doc.getRootElement().attributeValue("class");
        if (className == null)
            throw new Exception("Missing Attribute 'class' for Swiftlet '" + swiftletName + "'");
        return (Swiftlet) bundle.getBundleLoader().loadClass(className).newInstance();
    }

    /**
     * Returns the Swiftlet with the given name.
     * This method only returns Kernel Swiftlets. It is not possible to get Extension
     * Swiftlets due to different class loaders. If the Swiftlet is undefined or not started,
     * null is returned.
     *
     * @param swiftletName name of the Swiftlet, e.g. "sys$timer".
     * @return Swiftlet.
     */
    public Swiftlet getSwiftlet(String swiftletName) {
        Swiftlet swiftlet = null;
        swiftlet = swiftletTable.get(swiftletName);
        if (swiftlet != null && swiftlet.getState() == Swiftlet.STATE_ACTIVE && swiftlet.isKernel())
            return swiftlet;
        return null;
    }

    Swiftlet _getSwiftlet(String swiftletName) {
        Swiftlet swiftlet = null;
        swiftlet = swiftletTable.get(swiftletName);
        if (swiftlet != null && swiftlet.getState() == Swiftlet.STATE_ACTIVE)
            return swiftlet;
        return null;
    }

    /**
     * Returns the state of a Swiftlet.
     *
     * @param swiftletName name of the Swiftlet.
     * @return state.
     * @throws UnknownSwiftletException if the Swiftlet is undefined.
     */
    public final int getSwiftletState(String swiftletName) throws UnknownSwiftletException {
        Swiftlet swiftlet = null;
        swiftlet = swiftletTable.get(swiftletName);
        if (swiftlet == null)
            throw new UnknownSwiftletException("Swiftlet '" + swiftletName + "' is unknown");
        return swiftlet.getState();
    }

    /**
     * Checks if the Swiftlet is defined.
     *
     * @param swiftletName name of the Swiftlet.
     * @return true/false.
     */
    public final boolean isSwiftletDefined(String swiftletName) {
        return swiftletTable.containsKey(swiftletName);
    }

    private void stopAllSwiftlets() {
        lock.readLock().lock();
        try {
            trace("stopAllSwiftlets");
            List<Swiftlet> al = new ArrayList<Swiftlet>();
            for (Object o : RouterConfiguration.Singleton().getConfigurations().entrySet()) {
                Entity entity = (Entity) ((Map.Entry) o).getValue();
                if (entity instanceof Configuration) {
                    Configuration conf = (Configuration) entity;
                    if (conf.isExtension()) {
                        String name = conf.getName();
                        Swiftlet swiftlet = swiftletTable.get(name);
                        if (swiftlet != null && swiftlet.getState() == Swiftlet.STATE_ACTIVE) {
                            al.add(swiftlet);
                        }
                    }
                }
            }
            for (Swiftlet anAl : al) {
                Swiftlet swiftlet = anAl;
                trace("stopAllSwiftlets: Stopping swiftlet '" + swiftlet.getName() + "'");
                try {
                    shutdownSwiftlet(swiftlet);
                } catch (SwiftletException ignored) {
                }
                swiftlet.setStartupTime(-1);
                trace("stopAllSwiftlets: Swiftlet " + swiftlet.getName() + " has been stopped");
            }
        } finally {
            lock.readLock().unlock();
        }

    }

    public void shutdown(boolean removeShutdownHook) {
        if (removeShutdownHook && shutdownHook != null)
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        shutdown();
    }

    /**
     * Performs a shutdown of the router.
     */
    public void shutdown() {
        lock.writeLock().lock();
        try {
            System.out.println("Shutdown SwiftMQ " + Version.getKernelVersion() + " " + "[" + getRouterName() + "] ...");
            trace("shutdown");
            saveConfigIfDirty();
            if (configfileWatchdog != null)
                timerSwiftlet.removeTimerListener(configfileWatchdog);
            memoryMeter.close();
            stopAllSwiftlets();
            stopKernelSwiftlets();
            listeners.clear();
            allListeners.clear();
            kernelListeners.clear();
            swiftletTable.clear();
            RouterConfiguration.removeInstance();
            PoolManager.reset();
            traceSpace = null;
            logSwiftlet = null;
            System.out.println("Shutdown SwiftMQ " + Version.getKernelVersion() + " " + "[" + getRouterName() + "] DONE.");
        } finally {
            lock.writeLock().unlock();
        }

    }

    /**
     * Returns the name of this router
     *
     * @return router name.
     */
    public String getRouterName() {
        return routerName.get();
    }

    /**
     * Saves this router's configuration.
     */
    public void saveConfiguration() {
        lock.writeLock().lock();
        try {
            saveConfiguration(RouterConfiguration.Singleton());
            configDirty.set(false);
        } finally {
            lock.writeLock().unlock();
        }
    }

    protected Element[] getOptionalElements() {
        return null;
    }

    protected String[] saveConfiguration(RouterConfigInstance entity) {
        List<String> al = new ArrayList<String>();
        al.add(TreeCommands.INFO);
        try {
            String backupFile = configFilename + fmt.format(new Date());
            File file = new File(configFilename.get());
            file.renameTo(new File(backupFile));
            new NumberBackupFileReducer(file.getParent(), file.getName() + ".", 15).process();
            al.add("Configuration backed up to file '" + backupFile + "'.");
        } catch (Exception e) {
            al.add("Error creating configuration backup: " + e);
        }
        try {
            Document doc = DocumentHelper.createDocument();
            doc.addComment("  SwiftMQ Configuration. Last Save Time: " + new Date() + "  ");
            Element root = DocumentHelper.createElement("router");
            root.addAttribute("name", (String) entity.getEntity(Configuration.ENV_ENTITY).getProperty("routername").getValue());
            root.addAttribute("kernelpath", routerConfig.get().getRootElement().attributeValue("kernelpath"));
            root.addAttribute("release", Version.getKernelConfigRelease());
            root.addAttribute("startorder", routerConfig.get().getRootElement().attributeValue("startorder"));
            boolean b = (Boolean) entity.getEntity(Configuration.ENV_ENTITY).getProperty("use-smart-tree").getValue();
            if (!b)
                root.addAttribute("use-smart-tree", "false");
            long l = (Long) entity.getEntity(Configuration.ENV_ENTITY).getProperty("memory-collect-interval").getValue();
            if (l != 10000)
                root.addAttribute("memory-collect-interval", String.valueOf(l));
            Element[] optional = getOptionalElements();
            if (optional != null) {
                for (Element anOptional : optional) XMLUtilities.elementToXML(anOptional, root);
            }
            doc.setRootElement(root);
            Map configs = entity.getEntities();
            for (Object o : configs.keySet()) {
                Entity c = (Entity) configs.get((String) o);
                if (c instanceof Configuration)
                    XMLUtilities.configToXML((Configuration) c, root);
            }
            XMLUtilities.writeDocument(doc, configFilename.get());
            al.add("Configuration saved to file '" + configFilename + "'.");
        } catch (Exception e) {
            al.add("Error saving configuration: " + e);
        }
        return (String[]) al.toArray(new String[al.size()]);
    }

    private Configuration getConfigurationTemplate(String swiftletName) throws Exception {
        Bundle bundle = bundleTable.get(swiftletName);
        if (bundle == null)
            return null;
        Configuration c = XMLUtilities.createConfigurationTemplate(bundle.getBundleConfig());
        XMLUtilities.loadIcons(c, bundle.getBundleLoader());
        return c;
    }

    private Configuration fillConfiguration(Configuration template) throws Exception {
        return XMLUtilities.fillConfiguration(template, routerConfig.get());
    }

    Configuration fillConfigurationFromTemplate(String swiftletName, Document routerConfigDoc) throws Exception {
        Bundle bundle = bundleTable.get(swiftletName);
        if (bundle == null)
            return null;
        Configuration template = XMLUtilities.createConfigurationTemplate(bundle.getBundleConfig());
        return XMLUtilities.fillConfiguration(template, routerConfigDoc);
    }

    /**
     * Adds data to the survive data store.
     * Due to different class loaders of the Extension Swiftlets, it is not possible
     * to store data in static data structures within the Swiftlet that do survive a reboot (shutdown/restart)
     * of a router. Normally, a Swiftlet shouldn't have any data that must survive,
     * but there are some exceptions, e.g. server sockets which should be reused. This
     * kind of data can be registered within the <code>shutdown</code> method of a Swiftlet
     * under some key and fetched for reuse during the <code>startup</code> method.
     *
     * @param key  some key.
     * @param data the data
     */
    public void addSurviveData(String key, Object data) {
        surviveMap.put(key, data);
    }

    /**
     * Removes the survive data, stored under the given key.
     *
     * @param key key.
     */
    public void removeSurviveData(String key) {
        surviveMap.remove(key);
    }

    /**
     * Returns the survive data, stored under the given key.
     *
     * @param key key.
     * @return survive data.
     */
    public Object getSurviveData(String key) {
        return surviveMap.get(key);
    }

    /**
     * Adds a SwiftletManagerListener for a specific Swiftlet.
     *
     * @param swiftletName Swiftlet Name.
     * @param l            Listener.
     */
    public final void addSwiftletManagerListener(String swiftletName, SwiftletManagerListener l) {
        trace("addSwiftletManagerListener: Swiftlet " + swiftletName + "', adding SwiftletManagerListener");
        HashSet<SwiftletManagerListener> qListeners = listeners.get(swiftletName);
        if (qListeners == null) {
            qListeners = new HashSet<SwiftletManagerListener>();
            listeners.put(swiftletName, qListeners);
        }
        qListeners.add(l);
    }

    /**
     * Adds a SwiftletManagerListener for all Swiftlets.
     *
     * @param l Listener.
     */
    public final void addSwiftletManagerListener(SwiftletManagerListener l) {
        trace("addSwiftletManagerListener: adding SwiftletManagerListener");
        allListeners.add(l);
    }

    /**
     * Removes a SwiftletManagerListener for a specific Swiftlet.
     *
     * @param swiftletName Swiftlet Name.
     * @param l            Listener.
     */
    public final void removeSwiftletManagerListener(String swiftletName, SwiftletManagerListener l) {
        trace("removeSwiftletManagerListener: Swiftlet " + swiftletName + "', removing SwiftletManagerListener");
        HashSet<SwiftletManagerListener> qListeners = listeners.get(swiftletName);
        if (qListeners != null) {
            qListeners.remove(l);
            if (qListeners.isEmpty())
                listeners.put(swiftletName, null);
        }
    }

    /**
     * Removes a SwiftletManagerListener for all Swiftlets.
     *
     * @param l Listener.
     */
    public final void removeSwiftletManagerListener(SwiftletManagerListener l) {
        trace("removeSwiftletManagerListener: removing SwiftletManagerListener");
        allListeners.remove(l);
    }

    /**
     * Adds a KernelStartupListener.
     *
     * @param l Listener.
     */
    public final void addKernelStartupListener(KernelStartupListener l) {
        trace("addKernelStartupListener: adding KernelStartupListener");
        kernelListeners.add(l);
    }

    /**
     * Removes a KernelStartupListener.
     *
     * @param l Listener.
     */
    public final void removeKernelStartupListener(KernelStartupListener l) {
        trace("removeKernelStartupListener: removing KernelStartupListener");
        kernelListeners.remove(l);
    }

    protected void fireKernelStartedEvent() {
        trace("fireKernelStartedEvent");
        Set<? extends KernelStartupListener> cloned = null;
        cloned = (Set<? extends KernelStartupListener>) ((HashSet) kernelListeners).clone();
        for (KernelStartupListener aCloned : cloned) {
            (aCloned).kernelStarted();
        }
    }

    protected void fireSwiftletManagerEvent(String swiftletName, String methodName, SwiftletManagerEvent evt) {
        trace("fireSwiftletManagerEvent: Swiftlet " + swiftletName + "', method: " + methodName);

        // Notify listeners specific to the swiftlet
        Set<SwiftletManagerListener> qListeners = listeners.get(swiftletName);
        if (qListeners != null) {
            notifyListeners(qListeners, methodName, evt);
        }

        // Notify all listeners
        notifyListeners(allListeners, methodName, evt);
    }

    private void notifyListeners(Set<SwiftletManagerListener> listeners, String methodName, SwiftletManagerEvent evt) {
        for (SwiftletManagerListener listener : listeners) {
            switch (methodName) {
                case "swiftletStartInitiated":
                    listener.swiftletStartInitiated(evt);
                    break;
                case "swiftletStarted":
                    listener.swiftletStarted(evt);
                    break;
                case "swiftletStopInitiated":
                    listener.swiftletStopInitiated(evt);
                    break;
                case "swiftletStopped":
                    listener.swiftletStopped(evt);
                    break;
            }
        }
    }

    public class ConcurrentHashMapWithNulls<K, V> extends ConcurrentHashMap<K, V> {
        private final Object NULL_PLACEHOLDER = new Object();

        @SuppressWarnings("unchecked")
        public V put(K key, V value) {
            return value == null ? super.put(key, (V) NULL_PLACEHOLDER) : super.put(key, value);
        }

        public V get(Object key) {
            V value = super.get(key);
            return value == NULL_PLACEHOLDER ? null : value;
        }
    }

    private class SwiftletShutdown implements Runnable {
        Swiftlet swiftlet = null;
        SwiftletException exception = null;

        public SwiftletShutdown(Swiftlet swiftlet) {
            this.swiftlet = swiftlet;
        }

        public SwiftletException getException() {
            return exception;
        }

        public void run() {
            try {
                if (swiftlet.isKernel()) {
                    trace("Swiftlet " + swiftlet.getName() + "', fireSwiftletManagerEvent: swiftletStopInitiated");
                    fireSwiftletManagerEvent(swiftlet.getName(), "swiftletStopInitiated", new SwiftletManagerEvent(SwiftletManager.this, swiftlet.getName()));
                    trace("Swiftlet " + swiftlet.getName() + "', swiftlet.shutdown()");
                }
                swiftlet.shutdown();
                swiftlet.setState(Swiftlet.STATE_INACTIVE);
                if (swiftlet.isKernel()) {
                    trace("Swiftlet " + swiftlet.getName() + "', fireSwiftletManagerEvent: swiftletStopped");
                    fireSwiftletManagerEvent(swiftlet.getName(), "swiftletStopped", new SwiftletManagerEvent(SwiftletManager.this, swiftlet.getName()));
                }
            } catch (SwiftletException e) {
                exception = e;
            }
        }
    }
}

