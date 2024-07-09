package com.robomi.ros;

import com.robomi.store.VideoDataStore;
import org.ros.namespace.GraphName;
import org.ros.node.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNullApi;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;

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
//        try{
//            objNodeConfiguration.setMasterUri(new URI("http://192.168.123.126:11311"));
//        }catch (Exception e){
//            System.out.println("----------ROS MASTER Connect falied");
//        }
//        objNodeConfiguration.setNodeName("SubVideoNodeObject");
//
//
//        NodeConfiguration rtNodeConfiguration = NodeConfiguration.newPrivate();
//        try{
//            rtNodeConfiguration.setMasterUri(new URI("http://192.168.123.126:11311"));
//        }catch (Exception e){
//            System.out.println("----------ROS MASTER Connect falied");
//        }

        NodeConfiguration objNodeConfiguration = NodeConfiguration.newPrivate();
        objNodeConfiguration.setNodeName("SubVideoNodeObject");

        NodeConfiguration rtNodeConfiguration = NodeConfiguration.newPrivate();
        rtNodeConfiguration.setNodeName("SubVideoNodeRealtime");

        SubVideoNode objVideoNode = new SubVideoNode(applicationContext.getBean(VideoDataStore.class));
        objVideoNode.InitialNode("object", "/detect/image_raw", "sensor_msgs/Image", 100);
        objVideoNode.setNodeName("SubVideoNodeObject");

        SubVideoNode rtVideoNode = new SubVideoNode(applicationContext.getBean(VideoDataStore.class));
        rtVideoNode.InitialNode("realtime", "/realtime/image_raw", "sensor_msgs/Image", 100);
        rtVideoNode.setNodeName("SubVideoNodeRealtime");

        NodeMainExecutor nodeMainExecutor = DefaultNodeMainExecutor.newDefault();
        nodeMainExecutor.execute(rtVideoNode, rtNodeConfiguration);
        nodeMainExecutor.execute(objVideoNode, objNodeConfiguration);

        System.out.println("Video Sub Node Started");
    }
}
