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

    @Autowired
    public SubVideoNode(VideoDataStore store){
        this.videoDataStore = store;
    }

    public void InitialNode(String type, String topicName, String messageType, long captureTerm){
        System.out.println(topicName+" SubVideoNode ready -------------------------");
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
        System.out.println(topicName+" SubVideoNode start -------------------------");
        final Subscriber<Image> subscriber = connectedNode.newSubscriber(topicName, messageType);
        subscriber.addMessageListener(new MessageListener<Image>() {
            @Override
            public void onNewMessage(Image image) {
                long currentTime = System.currentTimeMillis();
                if(lastExecutionTime == 0) lastExecutionTime = currentTime;
                if(currentTime - lastExecutionTime >= captureTerm){
                    lastExecutionTime = currentTime;
                    ChannelBuffer buffer = image.getData();
                    byte[] imageData = new byte[buffer.readableBytes()];
                    buffer.readBytes(imageData);

                    int width = image.getWidth();
                    int height = image.getHeight();

                    System.out.println("width: "+width+" height: "+height);

                    String encoding = image.getEncoding();
                    byte[] decodeData = new byte[width * height * 3];
                    try {
                        byte[] data = image.getData().array();
                        for (int i = 0; i < width * height; i++) {
                            byte temp = decodeData[i * 3];
                            decodeData[i * 3] = data[i * 3 + 2]; // Red
                            decodeData[i * 3 + 1] = data[i * 3 + 1]; // Green
                            decodeData[i * 3 + 2] = temp; // Blue
                        }

                        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
                        bufferedImage.getRaster().setDataElements(0, 0, width, height, decodeData);

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(bufferedImage, "jpg", baos); // 이미지를 JPEG 형식으로 ByteArrayOutputStream에 쓰기
                        baos.flush();
                        byte[] byteArray = baos.toByteArray(); // ByteArrayOutputStream에서 byte 배열로 변환
                        baos.close();

                        DetectingObject detect = DetectingObject.getInstance();
                        if(type == "object"){
                            videoDataStore.insertObjQueue(byteArray);
                            detect.updateObject(byteArray, Main.getObjectInfoList());
                        } else{
                            videoDataStore.insertRtQueue(byteArray);
                            detect.realtime_camera_check(byteArray);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        super.onStart(connectedNode);
    }
}
