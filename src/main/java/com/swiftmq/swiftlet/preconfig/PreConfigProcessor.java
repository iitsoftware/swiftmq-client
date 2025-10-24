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

package com.swiftmq.swiftlet.preconfig;

import com.swiftmq.mgmt.XMLUtilities;
import com.swiftmq.swiftlet.timer.event.TimerListener;
import org.dom4j.Document;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified processor for preconfig files that can be used in two modes:
 * 1. Startup mode: with RouterConfigApplicator for direct XML manipulation
 * 2. Runtime mode: with ManagementTreeApplicator for live management tree updates
 * <p>
 * The processor handles file processing, directory monitoring, and change tracking.
 */
public class PreConfigProcessor implements TimerListener {
    private static final String PROP_WATCHDOG_DIR = "swiftmq.preconfig.watchdog.dir";
    private static final String PROP_WATCHDOG_INTERVAL = "swiftmq.preconfig.watchdog.interval";
    private static final String DEFAULT_DIR = "../preconfig-drop";
    private static final long DEFAULT_INTERVAL = 60000L;

    private AbstractApplicator applicator;
    private String watchdogDir = null;
    private long watchdogInterval = DEFAULT_INTERVAL;
    private Map<String, Long> processedFiles = new ConcurrentHashMap<>();
    private String context;
    private boolean logFileProcessing = true; // Log file processing during startup, not during intervals
    private SaveConfigCallback saveCallback = null;
    private String routerConfigFilename = null; // For RouterConfigApplicator saves

    /**
     * Create a PreConfigProcessor with the specified applicator and save callback.
     *
     * @param applicator   The applicator to use (RouterConfigApplicator or ManagementTreeApplicator)
     * @param context      Context name for logging
     * @param saveCallback Callback to save configuration after changes (only for ManagementTreeApplicator)
     */
    public PreConfigProcessor(AbstractApplicator applicator, String context, SaveConfigCallback saveCallback) {
        this.applicator = applicator;
        this.context = context;
        this.saveCallback = saveCallback;
        this.routerConfigFilename = null;
        this.watchdogDir = System.getProperty(PROP_WATCHDOG_DIR, DEFAULT_DIR);
        this.watchdogInterval = Long.parseLong(System.getProperty(PROP_WATCHDOG_INTERVAL, String.valueOf(DEFAULT_INTERVAL)));

        applicator.trace(context, "PreConfigProcessor initialized with dir=" + watchdogDir + ", interval=" + watchdogInterval + "ms");
        applicator.logInfo(context, "PreConfigProcessor initialized with dir=" + watchdogDir + ", interval=" + watchdogInterval + "ms");
    }

    /**
     * Create a PreConfigProcessor with RouterConfigApplicator and router config filename.
     *
     * @param applicator           The RouterConfigApplicator
     * @param context              Context name for logging
     * @param routerConfigFilename The router config file path for saving changes
     */
    public PreConfigProcessor(AbstractApplicator applicator, String context, String routerConfigFilename) {
        this.applicator = applicator;
        this.context = context;
        this.saveCallback = null;
        this.routerConfigFilename = routerConfigFilename;
        this.watchdogDir = System.getProperty(PROP_WATCHDOG_DIR, DEFAULT_DIR);
        this.watchdogInterval = Long.parseLong(System.getProperty(PROP_WATCHDOG_INTERVAL, String.valueOf(DEFAULT_INTERVAL)));

        applicator.trace(context, "PreConfigProcessor initialized with dir=" + watchdogDir + ", interval=" + watchdogInterval + "ms");
        applicator.logInfo(context, "PreConfigProcessor initialized with dir=" + watchdogDir + ", interval=" + watchdogInterval + "ms");
    }

    /**
     * Process a single preconfig file.
     *
     * @param preconfigFile Path to the preconfig file
     * @throws Exception if an error occurs during processing
     */
    public void processFile(String preconfigFile) throws Exception {
        File file = new File(preconfigFile);
        if (!file.exists()) {
            throw new Exception("Preconfig file not found: " + preconfigFile);
        }
        processFile(file);
    }

    /**
     * Process multiple preconfig files in order.
     *
     * @param preconfigFiles Comma-separated list of preconfig file paths
     * @throws Exception if an error occurs during processing
     */
    public void processFiles(String preconfigFiles) throws Exception {
        if (preconfigFiles == null || preconfigFiles.trim().length() == 0)
            return;

        String[] files = preconfigFiles.split(",");
        for (String file : files) {
            processFile(file.trim());
        }
    }

    /**
     * Process all preconfig files in the watchdog directory.
     * Files are processed in lexical order.
     * Creates the directory if it doesn't exist.
     */
    public void processDirectory() {
        File dir = new File(watchdogDir);

        // Create directory if it doesn't exist
        if (!dir.exists()) {
            applicator.trace(context, "Creating preconfig directory: " + watchdogDir);
            applicator.logInfo(context, "Creating preconfig directory: " + watchdogDir);
            if (!dir.mkdirs()) {
                applicator.logError(context, "Failed to create preconfig directory: " + watchdogDir);
                return;
            }
        }

        if (!dir.isDirectory()) {
            applicator.logError(context, "Preconfig path is not a directory: " + watchdogDir);
            return;
        }

        applicator.trace(context, "Processing preconfig directory: " + watchdogDir);
        applicator.logInfo(context, "Processing preconfig directory: " + watchdogDir);

        File[] files = dir.listFiles((d, name) -> name.endsWith(".xml"));
        if (files == null || files.length == 0) {
            applicator.trace(context, "No preconfig files found in directory");
            return;
        }

        // Sort files lexically by name
        Arrays.sort(files, Comparator.comparing(File::getName));

        for (File file : files) {
            try {
                processFile(file);
            } catch (Exception e) {
                applicator.logError(context, "Error processing file " + file.getName() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Process a single preconfig file.
     */
    private void processFile(File file) throws Exception {
        applicator.trace(context, "Processing preconfig file: " + file.getName());

        // Reset changes flag
        if (applicator instanceof ManagementTreeApplicator) {
            ((ManagementTreeApplicator) applicator).resetChangesFlag();
        } else if (applicator instanceof RouterConfigApplicator) {
            ((RouterConfigApplicator) applicator).resetChangesFlag();
        }

        // Load the preconfig document
        Document preconfigDoc = XMLUtilities.createDocument(new FileInputStream(file));

        // Apply using the injected applicator
        if (applicator instanceof RouterConfigApplicator) {
            RouterConfigApplicator rcApplicator = (RouterConfigApplicator) applicator;
            Document modifiedConfig = rcApplicator.applyPreConfig(preconfigDoc);

            // Save routerconfig.xml if changes were made
            if (rcApplicator.hasChangesMade() && routerConfigFilename != null) {
                applicator.trace(context, "Changes detected, saving routerconfig.xml");
                XMLUtilities.writeDocument(modifiedConfig, routerConfigFilename);
                applicator.logInfo(context, "Saved routerconfig.xml after preconfig changes");
            }
        } else if (applicator instanceof ManagementTreeApplicator) {
            ((ManagementTreeApplicator) applicator).applyPreConfig(preconfigDoc, context + "/" + file.getName());

            // Save configuration if changes were made
            ManagementTreeApplicator mtApplicator = (ManagementTreeApplicator) applicator;
            if (mtApplicator.hasChangesMade() && saveCallback != null) {
                applicator.trace(context, "Changes detected, saving configuration");
                applicator.logInfo(context, "Saving configuration after preconfig changes");
                saveCallback.saveConfiguration();
            }
        }

        // Remember this file and its modification time
        processedFiles.put(file.getAbsolutePath(), file.lastModified());

        applicator.trace(context, "Successfully processed preconfig file: " + file.getName());
        if (logFileProcessing) {
            applicator.logInfo(context, "Applied changes from preconfig file: " + file.getPath());
        }
    }

    /**
     * Timer callback - check for new or updated preconfig files in the watchdog directory.
     * Called periodically by the timer. Does not log file processing start/end, only operations.
     */
    public void performTimeAction() {
        File dir = new File(watchdogDir);

        if (!dir.exists() || !dir.isDirectory()) {
            applicator.trace(context, "Watchdog directory does not exist or is not a directory: " + watchdogDir);
            return;
        }

        applicator.trace(context, "Checking preconfig directory for changes");
        applicator.logInfo(context, "Checking preconfig directory for changes: " + watchdogDir);

        File[] files = dir.listFiles((d, name) -> name.endsWith(".xml"));
        if (files == null) {
            applicator.logInfo(context, "No files found in preconfig directory");
            return;
        }

        applicator.logInfo(context, "Found " + files.length + " XML file(s) in preconfig directory");

        // Sort files lexically by name
        Arrays.sort(files, Comparator.comparing(File::getName));

        // Disable file processing logging during interval checks
        // Operations (add/remove/clear/properties) will still log at INFO level
        boolean previousLogFileProcessing = logFileProcessing;
        logFileProcessing = false;

        try {
            int newFiles = 0;
            int updatedFiles = 0;
            int skippedFiles = 0;

            for (File file : files) {
                String filePath = file.getAbsolutePath();
                long currentModified = file.lastModified();
                Long previousModified = processedFiles.get(filePath);

                // Process if file is new or has been updated
                if (previousModified == null) {
                    applicator.trace(context, "New preconfig file detected: " + file.getName());
                    applicator.logInfo(context, "New preconfig file detected: " + file.getName());
                    try {
                        processFile(file);
                        newFiles++;
                    } catch (Exception e) {
                        applicator.logError(context, "Error processing new file " + file.getName() + ": " + e.getMessage());
                    }
                } else if (currentModified > previousModified) {
                    applicator.trace(context, "Updated preconfig file detected: " + file.getName());
                    applicator.logInfo(context, "Updated preconfig file detected: " + file.getName());
                    try {
                        processFile(file);
                        updatedFiles++;
                    } catch (Exception e) {
                        applicator.logError(context, "Error processing updated file " + file.getName() + ": " + e.getMessage());
                    }
                } else {
                    applicator.trace(context, "File already processed: " + file.getName());
                    skippedFiles++;
                }
            }

            applicator.logInfo(context, "Preconfig watchdog check complete: " + newFiles + " new, " + updatedFiles + " updated, " + skippedFiles + " unchanged");
        } finally {
            logFileProcessing = previousLogFileProcessing;
        }
    }

    /**
     * Start the timer-based watchdog. Call this after initial directory processing.
     * Logs the start of the watchdog at INFO level.
     */
    public void startWatchdog() {
        applicator.logInfo(context, "Starting preconfig watchdog with interval " + watchdogInterval + "ms");
    }

    /**
     * Get the watchdog directory path.
     */
    public String getWatchdogDir() {
        return watchdogDir;
    }

    /**
     * Get the watchdog interval in milliseconds.
     */
    public long getWatchdogInterval() {
        return watchdogInterval;
    }
}
