package com.robomi.controller;

import com.robomi.VideoWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/streaming")
public class VideoStreamingController {
    @Autowired
    private VideoWebSocketHandler videoWebSocketHandler;
    @GetMapping("/startVideoWebsocket")
    public String startVideoWebsocket(){
        try{
            videoWebSocketHandler.startVideoStreaming();
            return "Video Websocket started";
        } catch(Exception e){
            e.printStackTrace();
            return "Failed Video Websocket started: " + e.getMessage();
        }

    }

    @GetMapping("/stopVideoWebsocket")
    public String stopVideoWebsocket(){
        try {
            videoWebSocketHandler.stopVideoStreaming();
            return "Video Websocket stopped";
        } catch(Exception e) {
            e.printStackTrace();
            return "Failed to stop Video Websocket: " + e.getMessage();
        }
    }

    @GetMapping("/getCameraCount")
    public int getCameraCount(){
        try{
            return 1;
        } catch (Exception e){
            return 0;
        }
    }
}
