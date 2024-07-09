package com.robomi.object;

import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;

import java.util.ArrayList;
import java.util.List;

public class ObjectInfo {
    private List<Mat> imageMat = new ArrayList<>();
    private List<Mat> descriptors = new ArrayList<>(); // 전시물을 좌/좌앞/앞/우앞/우 5개의 사진이미지로 저장. 이미지 각각의 디스크립터
    private List<MatOfKeyPoint> keyPoints = new ArrayList<>();
    private String objectName;
    private OBJECT_STATUS objectStatus;
    private long lastCheckTime;
    private boolean sendToClient;

    public void addImageMat(Mat mat){imageMat.add(mat);}
    public void addDescriptor(Mat descriptor){descriptors.add(descriptor);}
    public void addKeypoint(MatOfKeyPoint key){keyPoints.add(key);}
    public void updateCheckTime(long time){lastCheckTime = time;}
    public void setObjectName(String name){objectName = name;}
    public void setObjectStatus(OBJECT_STATUS status){objectStatus = status;}
    public void setSendToClient(boolean isSend){sendToClient = isSend;}

    public List<Mat> getImageMat(){return imageMat;}
    public List<Mat> getDescriptors(){return descriptors;}
    public List<MatOfKeyPoint> getKeyPoints(){return keyPoints;}
    public long getLastCheckTime(){return lastCheckTime;}
    public String getObjectName(){return objectName;}
    public OBJECT_STATUS getObjectStatus(){return objectStatus;}
    public boolean getIsSendToClient(){return sendToClient;}
}
