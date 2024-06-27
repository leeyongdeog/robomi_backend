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

//        SubVideoNode objVideoNode = applicationContext.getBean(SubVideoNode.class);
//        objVideoNode.InitialNode("object","/ip129_usb_cam0/image_raw", "sensor_msgs/Image", 1000);

        SubVideoNode rtVideoNode = applicationContext.getBean(SubVideoNode.class);
        rtVideoNode.InitialNode("realtime", "/ip129_usb_cam0/image_raw", "sensor_msgs/Image", 100);

        NodeMainExecutor nodeMainExecutor = DefaultNodeMainExecutor.newDefault();
//        nodeMainExecutor.execute(objVideoNode, nodeConfiguration);
        nodeMainExecutor.execute(rtVideoNode, nodeConfiguration);

        System.out.println("Video Sub Node Started");
    }
}
