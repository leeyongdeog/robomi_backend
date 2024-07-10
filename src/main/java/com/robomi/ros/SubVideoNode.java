package com.robomi.ros;

import com.robomi.Main;
import com.robomi.MessageWebSocketHandler;
import com.robomi.VideoWebSocketHandler;
import com.robomi.object.DetectingObject;
import com.robomi.object.OBJECT_STATUS;
import com.robomi.object.ObjectInfo;
import com.robomi.store.VideoDataStore;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.ros.internal.message.MessageBuffers;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Subscriber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import sensor_msgs.Image;
import sensor_msgs.CompressedImage;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

@Component
public class SubVideoNode extends AbstractNodeMain {
    private final VideoDataStore videoDataStore;
    private String topicName;
    private String messageType;
    private long lastExecutionTime = 0;
    private long captureTerm = 1000;
    private String type;
    private String nodeName;

    @Autowired
    public SubVideoNode(VideoDataStore store){
        this.videoDataStore = store;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public void InitialNode(String type, String topicName, String messageType, long captureTerm){
        System.out.println(topicName+" SubVideoNode ready ---"+type);
        this.topicName = topicName;
        this.messageType = messageType;
        this.captureTerm = captureTerm;
        this.type = type;
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of(this.messageType);
    }

    public static byte[] readImageFileToByteArray(String imagePath) throws IOException {
        File imageFile = new File(imagePath);
        try (FileInputStream fis = new FileInputStream(imageFile);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }

            return bos.toByteArray();
        }
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        final Subscriber<CompressedImage> subscriber = connectedNode.newSubscriber(topicName, messageType);
        subscriber.addMessageListener(new MessageListener<CompressedImage>() {
            @Override
            public void onNewMessage(CompressedImage image) {
                long currentTime = System.currentTimeMillis();
                if(lastExecutionTime == 0) lastExecutionTime = currentTime;
                if(currentTime - lastExecutionTime >= captureTerm){
                    lastExecutionTime = currentTime;
                    ChannelBuffer buffer = image.getData();

                    int remainingBytes = buffer.readableBytes();
                    byte[] imageData = new byte[remainingBytes];
                    buffer.readBytes(imageData);

                    try {
                        ByteArrayInputStream bais = new ByteArrayInputStream(imageData);
                        BufferedImage bufferedImage = ImageIO.read(bais);

                        if (bufferedImage == null) {
                            System.out.println("BufferedImage is null. Possibly due to an unsupported format or corrupted data.");
                            return;
                        }

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(bufferedImage, "jpg", baos); // 이미지를 JPEG 형식으로 ByteArrayOutputStream에 쓰기
                        baos.flush();
                        byte[] byteArray = baos.toByteArray(); // ByteArrayOutputStream에서 byte 배열로 변환
                        baos.close();

                        DetectingObject detect = DetectingObject.getInstance();

                        if (type.equals("object")) {
                            videoDataStore.insertObjQueue(byteArray);
                            detect.updateObject(byteArray, Main.getObjectInfoList());
                        } else {
                            videoDataStore.insertRtQueue(byteArray);
                            detect.realtime_camera_check(byteArray);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }


//                    int width = image.getWidth();
//                    int height = image.getHeight();
//
////                    System.out.println("width: "+width+" height: "+height);
//
//                    String encoding = image.getEncoding();
//                    byte[] decodeData = new byte[width * height * 3];
//                    try {
//                        byte[] data = image.getData().array();
//                        for (int i = 0; i < width * height; i++) {
//                            byte temp = decodeData[i * 3];
//                            decodeData[i * 3] = data[i * 3 + 2]; // Red
//                            decodeData[i * 3 + 1] = data[i * 3 + 1]; // Green
//                            decodeData[i * 3 + 2] = temp; // Blue
//                        }
//
//                        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_BGR);
//                        bufferedImage.getRaster().setDataElements(0, 0, width, height, decodeData);
//
//                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                        ImageIO.write(bufferedImage, "jpg", baos); // 이미지를 JPEG 형식으로 ByteArrayOutputStream에 쓰기
//                        baos.flush();
//                        byte[] byteArray = baos.toByteArray(); // ByteArrayOutputStream에서 byte 배열로 변환
//                        baos.close();
//
//                        DetectingObject detect = DetectingObject.getInstance();
//
//                        if(type == "object"){
//                            videoDataStore.insertObjQueue(byteArray);
//                            detect.updateObject(byteArray, Main.getObjectInfoList());
//                        } else{
//                            videoDataStore.insertRtQueue(byteArray);
//                            detect.realtime_camera_check(byteArray);
//                        }
//
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
                }
            }
        });
        super.onStart(connectedNode);
    }

    public static byte[] convertYUYVtoRGB(byte[] yuyvData, int width, int height) {
        byte[] rgbData = new byte[width * height * 3];
        int index = 0;

        for (int i = 0; i < yuyvData.length; i += 4) {
            int y0 = yuyvData[i] & 0xff;
            int u0 = yuyvData[i + 1] & 0xff;
            int y1 = yuyvData[i + 2] & 0xff;
            int v0 = yuyvData[i + 3] & 0xff;

            byte[] rgb0 = yuvToRgb(y0, u0, v0);
            byte[] rgb1 = yuvToRgb(y1, u0, v0);

            if (index + 6 <= rgbData.length) { // Check array bounds
                System.arraycopy(rgb0, 0, rgbData, index, 3);
                System.arraycopy(rgb1, 0, rgbData, index + 3, 3);
                index += 6; // Move index by 6 bytes
            } else {
                System.err.println("Index out of bounds: " + index + " / " + rgbData.length);
                break; // Exit the loop or handle the out-of-bounds situation
            }
        }

        return rgbData;
    }


    private static byte[] yuvToRgb(int y, int u, int v) {
        int c = y - 16;
        int d = u - 128;
        int e = v - 128;

        int r = (298 * c + 409 * e + 128) >> 8;
        int g = (298 * c - 100 * d - 208 * e + 128) >> 8;
        int b = (298 * c + 516 * d + 128) >> 8;

        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));

        return new byte[] {(byte) r, (byte) g, (byte) b};
    }
}
