package com.swiftmq.preconfig;

import com.swiftmq.mgmt.PreConfigurator;
import com.swiftmq.mgmt.XMLUtilities;
import org.dom4j.Document;

import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;

public class Processor {
    private static SimpleDateFormat fmt = new SimpleDateFormat(".yyyyMMddHHmmssSSS");

    private static void checkAndApplyPreconfig(String preconfig, String configFilename) throws Exception {
        Document routerConfig = XMLUtilities.createDocument(new FileInputStream(configFilename));
        if (preconfig != null && preconfig.trim().length() > 0) {
            StringTokenizer t = new StringTokenizer(preconfig, ",");
            while (t.hasMoreTokens()) {
                String pc = t.nextToken();
                XMLUtilities.writeDocument(routerConfig, configFilename + fmt.format(new Date()));
                routerConfig = new PreConfigurator(routerConfig, XMLUtilities.createDocument(new FileInputStream(pc))).applyChanges();
                XMLUtilities.writeDocument(routerConfig, configFilename);
                System.out.println("Applied changes from preconfig file: " + pc);
            }
        }
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: Processor <preconfigfiles comma-separated> <routerconfigfile>");
            System.exit(0);
        }
        try {
            checkAndApplyPreconfig(args[0], args[1]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}