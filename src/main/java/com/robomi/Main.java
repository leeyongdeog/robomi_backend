package com.robomi;

import com.robomi.dto.ObjectDTO;
import com.robomi.object.OBJECT_STATUS;
import com.robomi.object.ObjectInfo;
import com.robomi.ros.RosMasterNode;
import com.robomi.service.ObjectService;
import com.robomi.store.VideoDataStore;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.global.opencv_core;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.BRISK;
import org.opencv.features2d.ORB;
import org.opencv.features2d.SIFT;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.xfeatures2d.SURF;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.robomi.controller",
        "com.robomi.service",
        "com.robomi.websock",
        "com.robomi"
})
public class Main {
    private static RosMasterNode masterNode;
    private static VideoDataStore videoDataStore;
    private static VideoWebSocketHandler videoWebSocketHandler;
    private static List<ObjectInfo> objectInfoList = new ArrayList<>();

    @Autowired
    private ObjectService objectService;
    @Autowired
    private ResourceLoader resourceLoader;

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Main.class);
        application.addListeners(new ShutdownListener());
        ApplicationContext context = application.run(args);

        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        System.out.println("OpenCV loaded successfully. Version: " + Core.VERSION);

        Loader.load(opencv_core.class);
        System.out.println("JavaCV native libraries loaded successfully.");

        Main main = context.getBean(Main.class);
        main.initialize(context);
    }

    private static class ShutdownListener implements ApplicationListener<ContextClosedEvent>{
        @Override
        public void onApplicationEvent(ContextClosedEvent event) {
            System.out.println("JAVA Server Shutdown.");

        }
    }

    public void initialize(ApplicationContext context){
        videoDataStore = new VideoDataStore();
        RosMasterNode rosMasterNode = context.getBean(RosMasterNode.class);
        try {
            rosMasterNode.run();
        } catch (IOException e) {
            System.out.println("ROS Failed.");
        }

        LoadObjectInfo();
    }

    public void LoadObjectInfo(){
        List<ObjectDTO> objList = objectService.getAllObjects();
        System.out.println(objList.get(0).getName());

        String resourcePath="imagefolder/";

        for(ObjectDTO dto : objList){
            ObjectInfo obj = new ObjectInfo();
            obj.setObjectName(dto.getName());
            obj.setObjectStatus(OBJECT_STATUS.OK);
            obj.updateCheckTime(0);

            String imageUrl = dto.getImg_path();
            String filename = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
            String filePath = resourcePath + filename;
            System.out.println(filePath);
            try {
                downloadImageIfNotExists(imageUrl, filePath);
                Mat imageMat = Imgcodecs.imread(filePath, Imgcodecs.IMREAD_GRAYSCALE);

                if (imageMat.empty()) {
                    System.out.println("Failed to read image");
                } else {
                    MatOfKeyPoint keypoint = new MatOfKeyPoint();
                    Mat descriptor = new Mat();

                    computeKeypointAndDescriptorBySIFT(imageMat, keypoint, descriptor);

                    obj.addImageMat(imageMat);
                    obj.addDescriptor(descriptor);
                    obj.addKeypoint(keypoint);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            objectInfoList.add(obj);
        }
    }

    private void downloadImageIfNotExists(String imageUrl, String localFilePath) throws IOException {
        Path path = Paths.get(localFilePath);
        if (!Files.exists(path)) {
            downloadImage(imageUrl, localFilePath);
        } else {
            System.out.println("Image file already exists: " + localFilePath);
        }
    }
    private void downloadImage(String imageUrl, String localFilePath) throws IOException {
        URL url = new URL(imageUrl);
        Files.copy(url.openStream(), Paths.get(localFilePath));
        System.out.println("Image downloaded to: " + localFilePath);
    }

    public static void computeKeypointAndDescriptorByORB(Mat img, MatOfKeyPoint keypoints, Mat descriptors){
        ORB orb = ORB.create();
        orb.detectAndCompute(img, new Mat(), keypoints, descriptors);
    }

    public static void computeKeypointAndDescriptorBySURF(Mat img, MatOfKeyPoint keypoints, Mat descriptors) {
        double hessianThreshold = 400; // Hessian Threshold 예시
        int nOctaves = 4;              // 옥타브 수 예시
        int nOctaveLayers = 3;         // 옥타브 레이어 수 예시
        boolean extended = true;       // 확장된 디스크립터 사용 여부
        boolean upright = false;       // 직각(orientation) 무관 특징 사용 여부

        SURF surf = SURF.create(hessianThreshold, nOctaves, nOctaveLayers, extended, upright);
        surf.detectAndCompute(img, new Mat(), keypoints, descriptors);
        if (descriptors.type() != CvType.CV_32F) {
            descriptors.convertTo(descriptors, CvType.CV_32F);
        }
    }

    public static void computeKeypointAndDescriptorBySIFT(Mat img, MatOfKeyPoint keypoints, Mat descriptors) {
        int sift_nfeatures = 0;         // SIFT 추출할 최대 키포인트 수, 0은 제한 없음
        int sift_nOctaveLayers = 3;     // SIFT 옥타브 레이어 수
        double sift_contrastThreshold = 0.08; // SIFT 코너 검출 임계값
        double sift_edgeThreshold = 15.0;    // SIFT 엣지 임계값
        double sift_sigma = 1.6;        // SIFT 옥타브 시그마
        SIFT sift = SIFT.create(sift_nfeatures, sift_nOctaveLayers, sift_contrastThreshold, sift_edgeThreshold, sift_sigma);
        sift.detectAndCompute(img, new Mat(), keypoints, descriptors);
        if (descriptors.type() != CvType.CV_32F) {
            descriptors.convertTo(descriptors, CvType.CV_32F);
        }
    }

    public static void computeKeypointAndDescriptorByBRISK(Mat img, MatOfKeyPoint keypoints, Mat descriptors) {
        BRISK brisk = BRISK.create();
        brisk.setThreshold(60);
        brisk.detectAndCompute(img, new Mat(), keypoints, descriptors);
        if (descriptors.type() != CvType.CV_32F) {
            descriptors.convertTo(descriptors, CvType.CV_32F);
        }
    }

    public static void computeKeypointORB_DescriptorSURF(Mat img, MatOfKeyPoint keypoints, Mat descriptors) {
        ORB orb = ORB.create();
        orb.detect(img, keypoints);

        SURF surf = SURF.create();
        surf.compute(img, keypoints, descriptors);

        if (descriptors.type() != CvType.CV_32F) {
            descriptors.convertTo(descriptors, CvType.CV_32F);
        }
    }

    public static void computeKeypointORB_DescriptorSIFT(Mat img, MatOfKeyPoint keypoints, Mat descriptors) {
        ORB orb = ORB.create();
        orb.detect(img, keypoints);

        SIFT sift = SIFT.create();
        sift.compute(img, keypoints, descriptors);
        if (descriptors.type() != CvType.CV_32F) {
            descriptors.convertTo(descriptors, CvType.CV_32F);
        }
    }

    public static void computeKeypointAndDescriptorByComplexAlgorithm(Mat img, MatOfKeyPoint keypoints, Mat descriptors){
        double hessianThreshold = 400;  // SURF Hessian Threshold
        int nOctaves = 4;               // SURF 옥타브 수
        int nOctaveLayers = 3;          // SURF 옥타브 레이어 수
        boolean extended = true;        // SURF 확장된 디스크립터 사용 여부
        boolean upright = false;        // SURF 직각(orientation) 무관 특징 사용 여부

        // SURF 알고리즘으로 키포인트와 디스크립터 계산
        SURF surf = SURF.create(hessianThreshold, nOctaves, nOctaveLayers, extended, upright);
        MatOfKeyPoint surf_kp = new MatOfKeyPoint();
        Mat surf_desc = new Mat();
        surf.detectAndCompute(img, new Mat(), surf_kp, surf_desc);

        // SIFT 알고리즘으로 키포인트와 디스크립터 계산
        int sift_nfeatures = 0;         // SIFT 추출할 최대 키포인트 수, 0은 제한 없음
        int sift_nOctaveLayers = 3;     // SIFT 옥타브 레이어 수
        double sift_contrastThreshold = 0.04; // SIFT 코너 검출 임계값
        double sift_edgeThreshold = 10.0;    // SIFT 엣지 임계값
        double sift_sigma = 1.6;        // SIFT 옥타브 시그마
        SIFT sift = SIFT.create(sift_nfeatures, sift_nOctaveLayers, sift_contrastThreshold, sift_edgeThreshold, sift_sigma);
        MatOfKeyPoint sift_kp = new MatOfKeyPoint();
        Mat sift_desc = new Mat();
        sift.detectAndCompute(img, new Mat(), sift_kp, sift_desc);

        // 키포인트와 디스크립터 합치기
        Mat combined_descriptors = new Mat();
        descriptors.push_back(surf_desc);
        descriptors.push_back(sift_desc);
        descriptors.convertTo(combined_descriptors, CvType.CV_32F);

        // 모든 키포인트를 합치기
        keypoints.fromList(surf_kp.toList());
        keypoints.fromList(sift_kp.toList());
    }
    public static List<ObjectInfo> getObjectInfoList(){return objectInfoList;}
}

