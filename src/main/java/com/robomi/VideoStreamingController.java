package com.robomi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.websocket.Session;
import java.io.IOException;

@RestController
public class VideoStreamingController {
    @Autowired
    private VideoWebSocketServer videoWebSocketServer;

    @GetMapping("/startVideoWebsocket")
    public String startVideoWebsocket(){
        try{
            VideoWebSocketServer.startVideoStreaming();
            return "Video Websocket started";
        } catch(IOException | InterruptedException e){
            e.printStackTrace();
            return "Failed Video Websocket started: " + e.getMessage();
        }

    }

    @GetMapping("/stopVideoWebsocket")
    public String stopVideoWebsocket(){
        try {
            VideoWebSocketServer.stopVideoStreaming();
            return "Video Websocket stopped";
        } catch(IOException e) {
            e.printStackTrace();
            return "Failed to stop Video Websocket: " + e.getMessage();
        }
    }
}
