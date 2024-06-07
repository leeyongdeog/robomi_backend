package com.robomi;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.global.opencv_core;
import org.opencv.core.Core;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.robomi.controller",
        "com.robomi.service",
        "com.robomi.websock",
        "com.robomi"
})
public class Main {
    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        System.out.println("OpenCV loaded successfully. Version: " + Core.VERSION);
        Loader.load(opencv_core.class);
        System.out.println("JavaCV native libraries loaded successfully.");
        SpringApplication.run(Main.class, args);
    }
}
// Test