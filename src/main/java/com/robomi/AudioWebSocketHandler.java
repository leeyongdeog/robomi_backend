package com.robomi;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

@Component
public class AudioWebSocketHandler extends BinaryWebSocketHandler {
    private static final CopyOnWriteArrayList<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    private static List<byte[]> audioChunks = new ArrayList<>();
    private static List<byte[]> recordChunks = new ArrayList<>();
    private static AudioFormat audioFormat;
    private static final int DESIRED_CHUNK_SIZE = 100 * 1024; //

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);

        sessions.add(session);
        System.out.println("New Audio WebSocket connection: " + session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        System.out.println("Audio WebSocket connection closed: " + status);
        stopRecording();
        super.afterConnectionClosed(session, status);
    }


    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        super.handleBinaryMessage(session, message);

        try {
            byte[] audioData = message.getPayload().array();
            System.out.print("Audio message: ");
            for (byte b : audioData) {
                System.out.print(b + " ");
            }
            System.out.println();

            if (audioFormat == null) {
                audioFormat = new AudioFormat(22000, 16, 1, true, false);
            }

            audioChunks.add(audioData);
            recordChunks.add(audioData);

            if (getTotalAudioLength() >= DESIRED_CHUNK_SIZE) {
                playAudioChunks();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(session.getId() + " message: " + e.getMessage());
        }
    }


//---------------------------------------------------------------------------- 음성 수신 테스트용
    public void stopRecording() {
        try {
            byte[] audioData = concatByteArrays(recordChunks);
            ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
            AudioInputStream ais = new AudioInputStream(bais, new AudioFormat(44100, 16, 1, true, false), audioData.length);
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, new File("recorded_audio.wav"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private byte[] concatByteArrays(List<byte[]> arrays) {
        int totalLength = arrays.stream().mapToInt(array -> array.length).sum();
        byte[] result = new byte[totalLength];
        int destPos = 0;
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, result, destPos, array.length);
            destPos += array.length;
        }
        return result;
    }

    private static int getTotalAudioLength() {
        int totalLength = 0;
        for (byte[] chunk : audioChunks) {
            totalLength += chunk.length;
        }
        return totalLength;
    }
    private static void playAudioChunks() {
        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(audioFormat);
            line.start();

            for (byte[] chunk : audioChunks) {
                line.write(chunk, 0, chunk.length);
            }

            line.drain();
            line.close();

            audioChunks.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
//    ---------------------------------------------------------------------------------------------

//    ----------------------------------------------------------------- 서버에서 클라이언트로 스트리밍 시
    public static void startAudioStreaming() throws Exception {
        while (!sessions.isEmpty()) {
            for (WebSocketSession session : sessions) {
                byte[] audioData = getAudioData();
                if (session.isOpen()) {
                    new AudioWebSocketHandler().sendAudio(audioData);
                }
            }
        }
    }
    public static void stopAudioStreaming() throws Exception {
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                session.close();
            }
        }
        ;
        sessions.clear();
    }
    public static void sendAudio(byte[] audioData) throws Exception {
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                session.sendMessage(new BinaryMessage(audioData));
            }
        }
    }
//----------------------------------------------------------------------

//    --------------------------------------------------------------------Topic에서 보내주는 오디오 데이터 얻어오는곳
    private static byte[] getAudioData() throws Exception {
        return new byte[0];
    }


}