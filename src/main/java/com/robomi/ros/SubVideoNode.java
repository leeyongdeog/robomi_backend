package com.robomi.ros;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.ros.internal.message.MessageBuffers;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Subscriber;
import sensor_msgs.Image;

public class SubVideoNode extends AbstractNodeMain {
    private final String topicName;
    private final String nodeName;

    public SubVideoNode(String topicName, String nodeName){
        this.topicName = topicName;
        this.nodeName = nodeName;
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of(this.nodeName);
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        final Subscriber<Image> subscriber = connectedNode.newSubscriber(topicName, nodeName);
        subscriber.addMessageListener(new MessageListener<Image>() {
            @Override
            public void onNewMessage(Image image) {
                connectedNode.getLog().info("Topic image timestemp: " + image.getHeader().getStamp());

                String encoding = image.getEncoding();
                ChannelBuffer imageData = image.getData();

                byte[] byteArray = new byte[imageData.readableBytes()];
                imageData.getBytes(0, byteArray);

                // 클라이언트로 보낼 데이터
            }
        });

        super.onStart(connectedNode);

    }
}
