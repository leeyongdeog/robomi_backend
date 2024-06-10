package com.robomi;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.global.opencv_core;
import org.opencv.core.Core;
import org.ros.node.Node;
import org.ros.node.NodeConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import java.net.URI;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.robomi.controller",
        "com.robomi.service",
        "com.robomi.websock",
        "com.robomi"
})
public class Main {
    private static final NodeConfiguration getNodeConfiguration(final String rosHostIP, final String nodeName, final URI rowMasterUri){
        final NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(rosHostIP);
        nodeConfiguration.setNodeName(nodeName);
        nodeConfiguration.setMasterUri(rowMasterUri);
        return nodeConfiguration;
    }

    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        System.out.println("OpenCV loaded successfully. Version: " + Core.VERSION);

        Loader.load(opencv_core.class);
        System.out.println("JavaCV native libraries loaded successfully.");



        SpringApplication.run(Main.class, args);
    }
}
// Test