package com.robomi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
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
    private static final int cam_number = 1;
    private static final CopyOnWriteArrayList<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    private static List<OpenCVFrameGrabber> grabberList = new ArrayList<>(cam_number);
    private static boolean[] cameraStarted = new boolean[cam_number];
    private static final OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);
    private static final Java2DFrameConverter converter = new Java2DFrameConverter();

    static {
        try {
            initialize_grabber();
            startAllCameras();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int getCameraCount() throws Exception{
        return cam_number;
    }

    static void initialize_grabber() throws Exception{
        for(int i=0; i<cam_number; ++i){
            OpenCVFrameGrabber g = new OpenCVFrameGrabber(0);
            grabberList.add(g);
            cameraStarted[i] = false;
        }
    }

    public static void startAllCameras() throws Exception{
        for (int i = 0; i < grabberList.size(); i++) {
            if (!cameraStarted[i]) {
                grabberList.get(i).start();
                cameraStarted[i] = true;
            }
        }
    }

    public static void stopAllCameras() throws Exception{
        for (int i = 0; i < grabberList.size(); i++) {
            if (cameraStarted[i]) {
                grabberList.get(i).stop();
                cameraStarted[i] = false;
            }
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        sessions.add(session);
        System.out.println("New Video WebSocket connection: " + session.getId());
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
//        System.out.println(jsonData);
        TextMessage message = new TextMessage(jsonData);
        for(WebSocketSession session : sessions){
            if(session.isOpen()){
                session.sendMessage(message);
            }
        }
    }

    public static void startVideoStreaming() throws Exception{
        while(!sessions.isEmpty()){
            for(int i=0; i<cam_number; ++i){
                if(cameraStarted[i]){
                    byte[] frameData = getVideoFrame(i);
                    for(WebSocketSession session : sessions){
                        if(session.isOpen()){
                            new VideoWebSocketHandler().sendVideoFrame(i, frameData);
                        }
                    }
                }
            }
            Thread.sleep(100);
        }
    }

    public static void stopVideoStreaming() throws Exception{
        for(WebSocketSession session : sessions){
            if(session.isOpen()){
                session.close();
            }
        };
        stopAllCameras();
        sessions.clear();
    }

    private static byte[] getVideoFrame(int index) throws Exception{
        if (index >= 0 && index < grabberList.size() && cameraStarted[index])
        {
            Frame frame = grabberList.get(index).grab();
            BufferedImage bufferedImage = converter.convert(frame);
            try(ByteArrayOutputStream bao = new ByteArrayOutputStream()){
                ImageIO.write(bufferedImage, "jpg", bao);
                return bao.toByteArray();
            }
        } else{
            return new byte[0];
        }
    }

    private static String createJsonData(int index, byte[] frameData) throws Exception{
        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("cameraIndex", index);
        jsonMap.put("frameData", Base64.getEncoder().encodeToString(frameData));
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(jsonMap);
    }
}
