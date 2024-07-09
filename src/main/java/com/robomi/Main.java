package com.robomi;

import com.robomi.dto.ManagerDTO;
import com.robomi.dto.ObjectDTO;
import com.robomi.object.OBJECT_STATUS;
import com.robomi.object.ObjectInfo;
import com.robomi.ros.RosMasterNode;
import com.robomi.service.ManagerService;
import com.robomi.service.ObjectService;
import com.robomi.store.VideoDataStore;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.global.opencv_core;
import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.face.LBPHFaceRecognizer;
import org.opencv.features2d.BRISK;
import org.opencv.features2d.ORB;
import org.opencv.features2d.SIFT;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.awt.image.BufferedImage;

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

    private CascadeClassifier faceCascade;
    private LBPHFaceRecognizer faceRecognizer;

    @Autowired
    private ObjectService objectService;
    @Autowired
    private ResourceLoader resourceLoader;
    @Autowired
    private ManagerService managerService;

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
        LoadManagers();



    }

    public void LoadManagers(){
        List<ManagerDTO> managerList = managerService.getAllManagers();
        loadHaarCascade();
        augmentAndTrainImages(managerList);
    }

    private void loadHaarCascade(){
        try {
            try {
                // XML 파일 경로
                String xmlFilePath = "libs/haarcascade_frontalface_default.xml";
                // CascadeClassifier 초기화
                faceCascade = new CascadeClassifier();
                faceCascade.load(xmlFilePath);
                System.out.println("OpenCV Haar-Cascade Load Success!!");
            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Mat downloadAndReadImage(String imageUrl) {
        String tempFilePath = "temp_imagefolder/temp_image.jpg";
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(imageUrl);
            HttpResponse response = httpClient.execute(request);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                try (FileOutputStream fos = new FileOutputStream(tempFilePath)) {
                    entity.writeTo(fos);
                }
                Mat image = Imgcodecs.imread(tempFilePath);
                if (!image.empty()) {
                    Mat grayImage = new Mat();
                    Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);
                    return grayImage;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new Mat(); // 빈 Mat 반환
    }

    /// 이미지 변형 및 학습
    private void augmentAndTrainImages(List<ManagerDTO> managerList) {
        List<Mat> allImages = new ArrayList<>();
        List<Integer> allLabels = new ArrayList<>();
        for (ManagerDTO manager : managerList) {
            Mat originalMat = downloadAndReadImage(manager.getImg_path());
            if (!originalMat.empty()) {
                // 좌 20도 회전
                Mat leftRotatedMat = rotateImage(originalMat, -20);
                saveMat(leftRotatedMat, "temp_imagefolder/"+manager.getName() + "_left_rotated.jpg");
                allImages.add(leftRotatedMat);
                allLabels.add(manager.getSeq().intValue());

                // 우 20도 회전
                Mat rightRotatedMat = rotateImage(originalMat, 20);
                saveMat(rightRotatedMat, "temp_imagefolder/"+manager.getName() + "_right_rotated.jpg");
                allImages.add(rightRotatedMat);
                allLabels.add(manager.getSeq().intValue());

                // 원본 사진
                saveMat(originalMat, "temp_imagefolder/"+manager.getName() + "_original.jpg");
                allImages.add(originalMat);
                allLabels.add(manager.getSeq().intValue());

                // 밝기를 30% 떨어뜨린 사진
                Mat darkenedMat = changeBrightness(originalMat, -30);
                saveMat(darkenedMat, "temp_imagefolder/"+manager.getName() + "_darkened.jpg");
                allImages.add(darkenedMat);
                allLabels.add(manager.getSeq().intValue());

                // 밝기를 30% 올린 사진
                Mat brightenedMat = changeBrightness(originalMat, 30);
                saveMat(brightenedMat, "temp_imagefolder/"+manager.getName() + "_brightened.jpg");
                allImages.add(brightenedMat);
                allLabels.add(manager.getSeq().intValue());
            }
            else{
                System.out.println("origin mat empty");
            }
        }

        // 모든 이미지를 FaceRecognition 모델에 학습시킴
        trainFaceRecognitionModel(allImages, allLabels);
    }

    // 이미지의 밝기를 조정하는 메서드
    private Mat changeBrightness(Mat inputMat, int value) {
        Mat processedMat = new Mat(inputMat.rows(), inputMat.cols(), inputMat.type());

        // 밝기 조정
        Core.add(inputMat, new Scalar(value, value, value), processedMat);

        return processedMat;
    }

    // 이미지 회전
    private Mat rotateImage(Mat source, double angle) {
        Mat rotated = new Mat();
        Point center = new Point(source.cols() / 2, source.rows() / 2);
        Mat rotationMatrix = Imgproc.getRotationMatrix2D(center, angle, 1.0);
        Imgproc.warpAffine(source, rotated, rotationMatrix, source.size());
        return rotated;
    }

    // 이미지 저장
    private void saveMat(Mat image, String fileName) {
        Imgcodecs.imwrite(fileName, image);
    }

    // 얼굴 인식 모델 학습
    private void trainFaceRecognitionModel(List<Mat> images, List<Integer> labels) {
        faceRecognizer = LBPHFaceRecognizer.create();

        // 레이블을 MatOfInt로 변환
        Mat labelsMat = new Mat(labels.size(), 1, CvType.CV_32SC1);
        int[] labelsArray = new int[labels.size()];
        for (int i = 0; i < labels.size(); i++) {
            labelsArray[i] = labels.get(i);
        }
        labelsMat.put(0, 0, labelsArray);

        System.out.println("images: "+images.size()+" labels: "+labelsMat.size());
        // 학습
        faceRecognizer.train(images, labelsMat);
        faceRecognizer.save("libs/face_recognition_model.xml");
        System.out.println("Face recognition model training completed.");
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

