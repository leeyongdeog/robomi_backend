package com.robomi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.robomi.store.VideoDataStore;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class VideoWebSocketHandler extends TextWebSocketHandler {
    private final VideoDataStore videoDataStore;
    private static final int cam_number = 1;
    private static final CopyOnWriteArrayList<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    private static VideoWebSocketHandler instance;
    private static boolean[] cameraStarted = new boolean[cam_number];

    private static final Java2DFrameConverter converter = new Java2DFrameConverter();

    @Autowired
    public VideoWebSocketHandler(VideoDataStore videoDataStore){
        this.videoDataStore = videoDataStore;
    }

    public static VideoWebSocketHandler getInstance(VideoDataStore videoDataStore) {
        if (instance == null) {
            instance = new VideoWebSocketHandler(videoDataStore);
        }
        return instance;
    }

    public static int getCameraCount() throws Exception{
        return cam_number;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        sessions.add(session);
        System.out.println("New Video WebSocket connection: " + session.getId());
        cameraStarted[0] = true;
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        System.out.println("Video WebSocket connection closed: " + session.getId());
        super.afterConnectionClosed(session, status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        super.handleTextMessage(session, message);
        System.out.println("Video Received message: " + message.getPayload());
    }

    public void sendVideoFrame(int index, byte[] frameData) throws Exception{
        String jsonData = createJsonData(index, frameData);
        TextMessage message = new TextMessage(jsonData);
        for(WebSocketSession session : sessions){
            if(session.isOpen()){
                session.sendMessage(message);
            }
        }
    }

    public void startVideoStreaming() throws Exception{
        VideoWebSocketHandler handler = VideoWebSocketHandler.getInstance(videoDataStore);
        while(!sessions.isEmpty()){
            for(int i=0; i<cam_number; ++i){
                if(cameraStarted[i]){
                    byte[] frameData = getVideoFrame(i);
                    for(WebSocketSession session : sessions){
                        if(session.isOpen()){
                            handler.sendVideoFrame(i, frameData);
                        }
                    }
                }
            }
            Thread.sleep(100);
        }
    }

    public void stopVideoStreaming() throws Exception{
        for(WebSocketSession session : sessions){
            if(session.isOpen()){
                session.close();
            }
        };
        sessions.clear();
    }

    private byte[] getVideoFrame(int index) throws Exception{
        return videoDataStore.popRtQueue();
    }

    private static String createJsonData(int index, byte[] frameData) throws Exception{
        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("cameraIndex", index);
        jsonMap.put("frameData", Base64.getEncoder().encodeToString(frameData));
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(jsonMap);
    }

    public void sendTiltStatus(String status) throws Exception {
        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("tiltStatus", status);
        ObjectMapper mapper = new ObjectMapper();
        String jsonData = mapper.writeValueAsString(jsonMap);
        TextMessage message = new TextMessage(jsonData);

        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                session.sendMessage(message);
            }
        }
    }
}
