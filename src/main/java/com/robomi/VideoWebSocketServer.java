package com.robomi;

import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@ServerEndpoint("/video")
public class VideoWebSocketServer {
    private static final CopyOnWriteArrayList<VideoWebSocketServer> clients = new CopyOnWriteArrayList<>();
    private static final List<Session> sessions = new ArrayList<>();
    private Session session;

    @OnOpen
    public void onOpen(Session session){
        this.session = session;
        sessions.add(session);
        clients.add(this);
        System.out.println("---- Open WebSocket -- "+ session);
        System.out.println("Add Client Session -- "+ session);
    }

    @OnClose
    public void onClose(){
        clients.remove(this);
        sessions.remove(this.session);
        System.out.println("---- Close WebSocket -- "+ session);
    }

    @OnMessage
    public void onMessage(String message){

    }

    public void sendVideoFrame(byte[] frameData) throws IOException{
        String base64Data = Base64.getEncoder().encodeToString(frameData);
        for(VideoWebSocketServer client : clients){
            client.session.getBasicRemote().sendText(base64Data);
        }
    }

    public static void startVideoStreaming() throws IOException, InterruptedException{
        System.out.println("---- Start Video Streaming by WebSocket -- ");
        while(!clients.isEmpty()){
            byte[] frameData = getVideoFrame();
            for(VideoWebSocketServer client : clients){
                client.sendVideoFrame(frameData);
            }
            Thread.sleep(100);
        }
    }

    public static void stopVideoStreaming() throws IOException{
        System.out.println("---- Stop Video Streaming by WebSocket -- ");
        for (Session session : sessions) {
            if (session.isOpen()) {
                session.close();
            }
        }
        clients.clear();
        sessions.clear();
    }

    private static byte[] getVideoFrame(){
        return new byte[0];
    }
}
