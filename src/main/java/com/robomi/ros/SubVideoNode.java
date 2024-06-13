package com.robomi.ros;

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

@Component
public class SubVideoNode extends AbstractNodeMain {
    private final VideoDataStore videoDataStore;
    private final String topicName;
    private final String messageType;
    private long lastExecutionTime = 0;


    @Autowired
    public SubVideoNode(VideoDataStore store, @Value("${video.topic.name}")String topicName, @Value("${video.message.type}")String messageType){
        System.out.println("-----------------SubVideoNode constructor");
        this.topicName = topicName;
        this.messageType = messageType;
        this.videoDataStore = store;
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of(this.messageType);
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        final Subscriber<Image> subscriber = connectedNode.newSubscriber(topicName, messageType);
        subscriber.addMessageListener(new MessageListener<Image>() {
            @Override
            public void onNewMessage(Image image) {
                long currentTime = System.currentTimeMillis();
                if(lastExecutionTime == 0) lastExecutionTime = currentTime;

                if(currentTime - lastExecutionTime >= 50){
                    lastExecutionTime = currentTime;
                    ChannelBuffer buffer = image.getData();
                    byte[] imageData = new byte[buffer.readableBytes()];
                    buffer.readBytes(imageData);

                    int width = image.getWidth();
                    int height = image.getHeight();
                    String encoding = image.getEncoding();
                    byte[] decodeData = new byte[width * height * 3];
                    try {
                        byte[] data = image.getData().array();
                        for (int i = 0; i < width * height; i++) {
                            decodeData[i * 3] = data[i * 3 + 2]; // Red
                            decodeData[i * 3 + 1] = data[i * 3 + 1]; // Green
                            decodeData[i * 3 + 2] = data[i * 3]; // Blue
                        }

                        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
                        bufferedImage.getRaster().setDataElements(0, 0, width, height, decodeData);

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(bufferedImage, "jpg", baos); // 이미지를 JPEG 형식으로 ByteArrayOutputStream에 쓰기
                        baos.flush();
                        byte[] byteArray = baos.toByteArray(); // ByteArrayOutputStream에서 byte 배열로 변환
                        baos.close();

                        videoDataStore.insertQueue(byteArray);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        super.onStart(connectedNode);

    }
}
