package com.robomi;

import com.robomi.ros.RosMasterNode;
import com.robomi.ros.SubVideoNode;
import com.robomi.store.VideoDataStore;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.global.opencv_core;
import org.opencv.core.Core;
import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.Node;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.event.ContextClosedEvent;

import java.io.IOException;
import java.net.URI;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.robomi.controller",
        "com.robomi.service",
        "com.robomi.websock",
        "com.robomi"
})
public class Main {
    private static RosMasterNode masterNode;
    private static VideoDataStore videoDataStore;
    private static VideoWebSocketHandler videoWebSocketHandler;

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Main.class);
        application.addListeners(new ShutdownListener());
        ApplicationContext context = application.run(args);

        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        System.out.println("OpenCV loaded successfully. Version: " + Core.VERSION);

        Loader.load(opencv_core.class);
        System.out.println("JavaCV native libraries loaded successfully.");

        videoDataStore = new VideoDataStore();

        RosMasterNode rosMasterNode = context.getBean(RosMasterNode.class);
        try {
            rosMasterNode.run();
        } catch (IOException e) {
            System.out.println("ROS Failed.");
        }
    }

    private static class ShutdownListener implements ApplicationListener<ContextClosedEvent>{
        @Override
        public void onApplicationEvent(ContextClosedEvent event) {
            System.out.println("JAVA Server Shutdown.");

        }
    }
}


