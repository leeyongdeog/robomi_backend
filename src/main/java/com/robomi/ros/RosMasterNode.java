package com.robomi.ros;

import org.ros.namespace.GraphName;
import org.ros.node.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNullApi;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Component
public class RosMasterNode extends AbstractNodeMain implements ApplicationContextAware {
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("RosMaster_JAVA");
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        System.out.println("Ros Master Start");
    }

    @Override
    public void onShutdown(Node node) {

    }

    @Override
    public void onShutdownComplete(Node node) {

    }

    @Override
    public void onError(Node node, Throwable throwable) {

    }

    public void run() throws IOException{
        startVideoSubNode();
        System.out.println("Ros Master Started");
    }

    private void startVideoSubNode(){
        NodeConfiguration nodeConfiguration = NodeConfiguration.newPrivate();
        nodeConfiguration.setNodeName("SubVideoNode");

//        String topicName = "/ip129_usb_cam0/image_raw";
//        SubVideoNode subVideoNode = new SubVideoNode(topicName, "sensor_msgs/Image");
        SubVideoNode subVideoNode = applicationContext.getBean(SubVideoNode.class);

        NodeMainExecutor nodeMainExecutor = DefaultNodeMainExecutor.newDefault();
        nodeMainExecutor.execute(subVideoNode, nodeConfiguration);

        System.out.println("Video Sub Node Started");
    }
}
