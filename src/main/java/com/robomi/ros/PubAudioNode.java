package com.robomi.ros;

import audio_common_msgs.AudioData;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.topic.Publisher;

public class PubAudioNode extends AbstractNodeMain {
    private final String topicName;
    private final String nodeName;
    private static boolean isRunning = false;

    public PubAudioNode(String topicName, String nodeName){
        this.topicName = topicName;
        this.nodeName = nodeName;
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of(this.nodeName);
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        isRunning = true;
        final Publisher<AudioData> publisher = connectedNode.newPublisher(topicName,nodeName);

        new Thread(() -> {
            while(isRunning){
                byte[] audioData = new byte[0]; // 클라이언트에서 받은 데이터
                AudioData data = publisher.newMessage();
                ChannelBuffer channelBuffer = ChannelBuffers.wrappedBuffer(audioData);
                data.setData(channelBuffer);
                publisher.publish(data);
            }
        }).start();
        super.onStart(connectedNode);
    }

    @Override
    public void onShutdown(Node node) {
        isRunning = false;
        super.onShutdown(node);
    }
}
