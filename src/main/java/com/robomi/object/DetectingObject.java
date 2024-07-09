package com.robomi.object;

import com.robomi.Main;
import com.robomi.MessageWebSocketHandler;
import com.robomi.entity.CaptureEntity;
import com.robomi.entity.ObjectEntity;
import com.robomi.repository.CaptureRepo;
import com.robomi.rpc.CroppedData;
import com.robomi.rpc.ImageProcessor;
import com.robomi.rpc.ProcessedImage;
import com.robomi.service.CaptureService;
import com.robomi.service.S3Uploader;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.ndarray.Shape;
import org.tensorflow.ndarray.buffer.FloatDataBuffer;
import org.tensorflow.types.TFloat32;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.*;
import java.net.HttpURLConnection;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.net.URL;

public class DetectingObject {
    @Autowired
    private CaptureService captureService;

    @Value("${sound.server.url}")
    private String sound_server_url;

    public static long TURN_TIME = 3000; // 추후 실습시엔 로봇이 전시장전체를 1바퀴 도는 시간으로 설정.
    private static DetectingObject instance;
    private int GOODMATCH_THRESHOLD = 10;
    private double MATCH_THRESHOLD = 60;
    private int HOMOGRAPY_THRESHOLD = 5;
    private double TILT_THRESHOLD = 15.0;
    private double CUT_AREA_LR = 0.3;
    private double CUT_AREA_TB = 0.2;

    private List<DMatch> goodMatchList = new ArrayList<>();

    public static synchronized DetectingObject getInstance(){
        if(instance == null){
            instance = new DetectingObject();
        }
        return instance;
    }

    private MultipartFile convertMultipartFile(byte[] src, String objName){
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(src);
        String fileName = objName + System.currentTimeMillis();
        try{
            MultipartFile multipartFile = new MockMultipartFile(fileName, fileName, "image/jpeg", byteArrayInputStream);
            return multipartFile;
        } catch(Exception e){
            return null;
        }
    }

    private ProcessedImage communicationRPC(CroppedData cameraFrame){
        try{
            TTransport transport = new TSocket("localhost", 9090);
            transport.open();

            TProtocol protocol = new TBinaryProtocol(transport);
            ImageProcessor.Client client = new ImageProcessor.Client(protocol);

            ProcessedImage processedImage = client.processImage(cameraFrame);

            transport.close();

            return processedImage;
        } catch (TTransportException e){
            e.printStackTrace();
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    private List<Mat> cropByEdge(Mat src){
        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);

        Mat blurred = new Mat();
        Imgproc.GaussianBlur(gray, blurred, new Size(5, 5), 0);

        Mat edged = new Mat();
        Imgproc.Canny(blurred, edged, 50, 150);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(edged, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        // Get image dimensions
        int vwidth = src.width();
        int vheight = src.height();

        // List to store cropped images
        List<Mat> croppedDataList = new ArrayList<>();
        List<Rect> boxList = new ArrayList<>();

        // Iterate through contours
        for (int i = 0; i < contours.size(); i++) {
            MatOfPoint contour = contours.get(i);
            Rect boundingRect = Imgproc.boundingRect(contour);

            // Ignore small areas
            double area = boundingRect.width * boundingRect.height;
            if (area < vwidth * vheight * 0.01) {
                continue;
            }

            boxList.add(boundingRect);


        }

        List<Rect> newBoxList = new ArrayList<>();
        boolean[] merged = new boolean[boxList.size()];

        for(int i=0; i<boxList.size(); ++i){
            if(!merged[i]) {
                Point currentCenter = new Point(boxList.get(i).x + boxList.get(i).width / 2.0, boxList.get(i).y + boxList.get(i).height / 2.0);
                Rect currentBox = boxList.get(i);
                double currentWeight = currentBox.width * currentBox.height;

                for(int j=i+1; j<boxList.size(); ++j){
                    Point nextCenter = new Point(boxList.get(j).x + boxList.get(j).width / 2.0, boxList.get(j).y + boxList.get(j).height / 2.0);
                    double nextWeight = boxList.get(j).width * boxList.get(j).height;
                    double distance = Math.sqrt(Math.pow(currentCenter.x - nextCenter.x, 2) + Math.pow(currentCenter.y - nextCenter.y, 2));
                    if(distance < 200){
                        merged[j] = true;
                        int newX = Math.min(currentBox.x, boxList.get(j).x);
                        int newY = Math.min(currentBox.y, boxList.get(j).y);
                        int newW = Math.max(currentBox.width, boxList.get(j).width);
                        int newH = Math.max(currentBox.height, boxList.get(j).height);
                        currentBox.x = newX;
                        currentBox.y = newY;
                        currentBox.width = newW;
                        currentBox.height = newH;

                        double totalWeight = currentWeight + nextWeight;
                        double currentWeightRatio = currentWeight / totalWeight;
                        double nextWeightRatio = nextWeight / totalWeight;

                        // Calculate weighted centers
                        double weightedX = currentCenter.x * currentWeightRatio + nextCenter.x * nextWeightRatio;
                        double weightedY = currentCenter.y * currentWeightRatio + nextCenter.y * nextWeightRatio;

                        currentCenter = new Point(weightedX, weightedY);
                        currentWeight = totalWeight;
                    }
                }

                newBoxList.add(currentBox);
            }
        }

        for(int i=0; i<newBoxList.size(); ++i){
            // Crop the object from the original image
            Mat croppedImage = new Mat(src, newBoxList.get(i));

            // Check if the cropped image is not empty
            if (croppedImage.empty()) {
                System.out.println("crop empty ");
                continue;
            }

            // Add cropped image to the list
            croppedDataList.add(croppedImage);
        }

        return croppedDataList;
    }

    public byte[] realtime_camera_check(byte[] cameraByte){
        Mat cameraFrame = Imgcodecs.imdecode(new MatOfByte(cameraByte), Imgcodecs.IMREAD_COLOR);
        int width = cameraFrame.cols();
        int height = cameraFrame.rows();

        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".jpg", cameraFrame, buffer);

        CroppedData rpcData = new CroppedData();
        rpcData.setImageData(buffer.toArray());
        rpcData.setWidth(width);
        rpcData.setHeight(height);
        ProcessedImage croppedObjects = communicationRPC(rpcData);
        if(croppedObjects == null) return null;

//        System.out.println("RPC cropped: "+ croppedObjects.getCroppedDataSize());
//        System.out.println("RPC Status: "+ croppedObjects.getStatus());
        if(croppedObjects.getStatus().equals("default")) return null;
        // 검출표시된 이미지 알림으로 캡쳐로 전송
        // 클라이언트로 알림 메세지 전송
        byte[] retImg = new byte[0];
        List<CroppedData> cropDatas = croppedObjects.getCroppedData();
        for (int i = 0; i < cropDatas.size(); ++i) {
            CroppedData croppedData = cropDatas.get(i);
            System.out.println(croppedData);
            if(croppedData.imageData == null) continue;

            croppedData.imageData.rewind();  // ByteBuffer rewind() 호출

            byte[] byteArray = new byte[croppedData.imageData.remaining()];
            croppedData.imageData.get(byteArray);

            // MatOfByte로 이미지 데이터 디코딩
            Mat imgMat = Imgcodecs.imdecode(new MatOfByte(byteArray), Imgcodecs.IMREAD_GRAYSCALE);

            // Mat 객체가 비어 있는지(empty) 확인
            if (imgMat.empty()) {
                System.out.println("No data for cropped object " + i);
                return null;
            } else {
                // 이미지 파일로 저장
                String filename = "pyimg_" + i + ".png";
                Imgcodecs.imwrite(filename, imgMat);
                System.out.println("Saved image file: " + filename);
                retImg = byteArray;

                MultipartFile capture = convertMultipartFile(byteArray, "RealTime");
                if(capture != null){

                    try{
                        // 화면캡쳐 이미지 S3에 업로드
                        String keyName = UUID.randomUUID().toString();
                        String folderName = "captures";
                        String S3BucketName = "robomi-storage";
                        String region = "ap-northeast-2";
                        S3Uploader.uploadFile(S3BucketName, folderName, keyName, capture);
                        System.out.println("RealTime Alert upload capture to S3 success");

                        //DB capture 테이블 업데이트
                        String imgUrl = "https://" + S3BucketName + ".s3." + region + ".amazonaws.com/" +folderName + "/" + keyName + ".jpg";
                        captureService.addCapture("RealTime", imgUrl, 9l);

                        // 클라이언트에 전시물 상태 통보
                        String jsonStr = "{\"name\":"+"실시간감지"+", \"status\":"+"\""+"반입물품위반"+"\""+", \"time\":"+System.currentTimeMillis()+"}";
                        try {
                            MessageWebSocketHandler.getInstance().sendMessage(jsonStr);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        playSound();
                    } catch (Exception e){
                        System.out.println("upload capture to S3 failed");
                        e.printStackTrace();
                    }
                }

            }
        }
        return retImg;
    }

    public boolean updateObject(byte[] cameraByte, List<ObjectInfo> objectInfoList){
        boolean retStatus = false;

        Mat cameraFrame = Imgcodecs.imdecode(new MatOfByte(cameraByte), Imgcodecs.IMREAD_COLOR);
        int width = cameraFrame.cols();
        int height = cameraFrame.rows();

        int left = (int)(width * CUT_AREA_LR);
        int top = (int)(height * CUT_AREA_TB);
        int right = (int)(width * (1-CUT_AREA_LR));
        int bottom = (int)(height * (1-CUT_AREA_TB));

        Rect roi = new Rect(new org.opencv.core.Point(left, top), new org.opencv.core.Point(right, bottom));
        cameraFrame = new Mat(cameraFrame, roi);

        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".jpg", cameraFrame, buffer);

        List<Mat> croppedList = cropByEdge(cameraFrame);

        // 카메라에서 들어오는 이미지 키포인트와 디스크립터 계산
        List<MatOfKeyPoint> crop_kp_list = new ArrayList<>();
        List<Mat> crop_desc_list = new ArrayList<>();

        for(Mat crop : croppedList){
            MatOfKeyPoint keypoint = new MatOfKeyPoint();
            Mat descriptor = new Mat();

            Main.computeKeypointAndDescriptorBySIFT(crop, keypoint, descriptor);

            crop_kp_list.add(keypoint);
            crop_desc_list.add(descriptor);
        }

        for(ObjectInfo objectInfo : objectInfoList){
            // 마지막 체크시간이 로봇이 전시장 한바퀴 도는시간을 지나지 않았으면, 이 전시물은 비교 스킵 -> 불필요한 연산 제거
            if(System.currentTimeMillis() - objectInfo.getLastCheckTime() < TURN_TIME) continue;

            List<Mat> obj_descriptors = objectInfo.getDescriptors();
            List<Mat> obj_imageMats = objectInfo.getImageMat();
            List<MatOfKeyPoint> obj_keypoint = objectInfo.getKeyPoints();

            boolean findObject = false;
            for(int i=0; i<obj_descriptors.size(); ++i){
                for(int j = 0; j<crop_desc_list.size(); ++j){
                    goodMatchList = checkGoodMatch(obj_descriptors.get(i), crop_desc_list.get(j), MATCH_THRESHOLD);
                    if(goodMatchList.size() >= GOODMATCH_THRESHOLD){
                        System.out.println("------------------ test object DETECTED !!!!"+goodMatchList.size());

                        // 카메라 이미지 저장
                        String camImageFileName = "cam.jpg";
                        Imgcodecs.imwrite(camImageFileName, cameraFrame);

                        // 원본 이미지 저장
                        String orgImageFileName = "org.jpg";
                        Imgcodecs.imwrite(orgImageFileName, obj_imageMats.get(i));

                        // ----- 기울기 체크, 존재유무 체크, 파손유무 체크
                        OBJECT_STATUS status = checkStatus(goodMatchList,
                                obj_keypoint.get(i), crop_kp_list.get(j),
                                obj_imageMats.get(i), croppedList.get(j),
                                obj_descriptors.get(i), crop_desc_list.get(j));

                        findObject = true;
                        if(status == OBJECT_STATUS.UNKNOWN) continue;

                        objectInfo.setObjectStatus(status);
                        objectInfo.updateCheckTime(System.currentTimeMillis());

                        if(status != OBJECT_STATUS.OK){
                            MultipartFile capture = convertMultipartFile(cameraByte, objectInfo.getObjectName());
                            try{
                                // 화면캡쳐 이미지 S3에 업로드
                                String keyName = UUID.randomUUID().toString();
                                String folderName = "captures";
                                String S3BucketName = "robomi-storage";
                                String region = "ap-northeast-2";
                                S3Uploader.uploadFile(S3BucketName, folderName, keyName, capture);
                                System.out.println(objectInfo.getObjectName()+" upload capture to S3 success: "+status);


                                //DB capture 테이블 업데이트
                                String statusStr = status == OBJECT_STATUS.OK ? "OK"
                                        : status == OBJECT_STATUS.TILT ? "상태이상"
                                        : status == OBJECT_STATUS.DAMAGE ? "파손"
                                        : status == OBJECT_STATUS.LOST ? "분실"
                                        : "UNKNOWN";

                                String imgUrl = "https://" + S3BucketName + ".s3." + region + ".amazonaws.com/" +folderName + "/" + keyName + ".jpg";
                                captureService.addCapture(objectInfo.getObjectName(), imgUrl, status.toLong());

                                // 클라이언트에 전시물 상태 통보
                                String jsonStr = "{\"name\":"+objectInfo.getObjectName()+", \"status\":"+"\""+statusStr+"\""+", \"time\":"+objectInfo.getLastCheckTime()+"}";
                                try {
                                    MessageWebSocketHandler.getInstance().sendMessage(jsonStr);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                if(!retStatus) retStatus = true;
                            } catch (Exception e){
                                System.out.println("upload capture to S3 failed");
                                e.printStackTrace();
                            }
                        }
                        break;
                    }
                    else{
//                        System.out.println("------------------ test object not exist !!!!"+goodMatchList.size());
                    }
                }
            }
            if(findObject) break;
        }



        return retStatus;
    }

    private List<DMatch> countGoodMatches(MatOfDMatch matches, double threshold){
        List<DMatch> goods = new ArrayList<>();
        for(int i=0; i<matches.rows(); ++i){
            if(matches.toArray()[i].distance < threshold){
                goods.add(matches.toArray()[i]);
            }
        }
        return goods;
    }

    private List<DMatch> checkGoodMatch(Mat origin, Mat current, double threshold){
        if(origin.empty() || current.empty()) return new ArrayList<>(); // 빈 리스트 또는 다른 기본값 반환

        try {
            DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.FLANNBASED);
            MatOfDMatch matches = new MatOfDMatch();
            matcher.match(origin, current, matches);

            double matchThreshold = threshold;
            List<DMatch> goods = countGoodMatches(matches, matchThreshold);
            return goods;
        } catch (Exception e) {
            // 예외 처리
            e.printStackTrace(); // 예외 내용 출력
            return new ArrayList<>(); // 빈 리스트 또는 다른 기본값 반환
        }
    }

    private OBJECT_STATUS checkTilt(double[] homograpyData){
        OBJECT_STATUS status = OBJECT_STATUS.OK;
        double h00 = homograpyData[0], h01 = homograpyData[1], h10 = homograpyData[3], h11 = homograpyData[4];

        double angle = Math.toDegrees(Math.atan2(h01, h00));

        if (Math.abs(angle) > TILT_THRESHOLD && Math.abs(angle) <= 100.0 ) {
            status = OBJECT_STATUS.TILT;
            System.out.println("object tilt angle: " + angle);
        }
        return status;
    }

    private OBJECT_STATUS checkStatus(List<DMatch> matches,
                                      MatOfKeyPoint origin_keypoint,
                                      MatOfKeyPoint current_keypoint,
                                      Mat origin_img, Mat current_img,
                                      Mat origin_descriptor, Mat current_descriptor
    ){
        OBJECT_STATUS status = OBJECT_STATUS.OK;

        // 호모그래피 계산
        Mat homography = findHomography(origin_keypoint, current_keypoint, matches);
        if (homography.empty()) {
            System.out.println("homography empty");
            return OBJECT_STATUS.UNKNOWN;
        }

        double[] data = new double[9];
        homography.get(0, 0, data);

        status = checkTilt(data);

        BufferedImage bufImage11 = convertMatToBufferedImage(origin_img);
        BufferedImage bufImage21 = convertMatToBufferedImage(current_img);
        drawMatchesWithBoundingBox(bufImage11, origin_keypoint.toList(), bufImage21, current_keypoint.toList(), matches, "origin_matched_image.jpg");

        return status;
    }

    private void playSound(){
        String serverUrl = sound_server_url+"play_audio?file_name=sound.wav";

        // POST 요청 보낼 데이터 설정
        String audioFilename = "sound.wav";  // 재생할 오디오 파일명
        String postData = "audio_filename=" + audioFilename;

        try{
            URL url = new URL(serverUrl);
            // HttpURLConnection 객체 생성 및 설정
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Content-Type", "application/text");
            con.setDoOutput(true);

            // 응답 코드 확인
            int responseCode = con.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            // 응답 내용 확인
            try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                String inputLine;
                StringBuffer response = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                System.out.println("Response: " + response.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }

            // 연결 종료
            con.disconnect();
        }
        catch (Exception e){
            System.out.println("Sound play error" );
        }
    }

    private boolean detectDamage(List<KeyPoint> origin, List<KeyPoint> current, List<DMatch> goodMatch){
        // 파손 여부를 판단하기 위한 임계값
        System.out.println("resize keypoint: "+origin.size()+" camera keypoint: "+current.size()+" goods: "+goodMatch.size());
        int damagedMatchCount = 0;
        for (DMatch match : goodMatch) {
            Point pt1 = origin.get(match.queryIdx).pt;
            Point pt2 = current.get(match.trainIdx).pt;

            double distance = Math.sqrt(Math.pow(pt1.x - pt2.x, 2) + Math.pow(pt1.y - pt2.y, 2));
            System.out.println("damaged distance: "+distance);
            if (distance > 100) {
                damagedMatchCount++;
            }
        }

        System.out.println("damaged count: "+damagedMatchCount);

        // 전체 매치 중 파손된 매치의 비율이 임계값 이상이면 파손으로 판단
        double damagedRatio = (double) damagedMatchCount / goodMatch.size();
        return damagedRatio > 0.05; // 예: 전체 매치의 20% 이상이 파손으로 판단
    }

    private Mat findHomography(MatOfKeyPoint origin, MatOfKeyPoint current, List<DMatch> matches){
        List<KeyPoint> origin_kp = origin.toList();
        List<KeyPoint> current_kp = current.toList();

        if (matches.size() < 4) {
            return new Mat(); // 매치된 키포인트 쌍이 4개 미만일 경우 빈 행렬 반환
        }

        List<DMatch> validMatches = filterMatches(origin_kp, current_kp, matches);

        MatOfPoint2f origin_pts = new MatOfPoint2f();
        MatOfPoint2f current_pts = new MatOfPoint2f();

        try {
            for (DMatch match : validMatches) {
                KeyPoint originPoint = origin_kp.get(match.queryIdx);
                KeyPoint currentPoint = current_kp.get(match.trainIdx);
                origin_pts.push_back(new MatOfPoint2f(originPoint.pt));
                current_pts.push_back(new MatOfPoint2f(currentPoint.pt));
            }

            return Calib3d.findHomography(origin_pts, current_pts, Calib3d.RANSAC, HOMOGRAPY_THRESHOLD);
        } catch (Exception e) {
            e.printStackTrace();
            return new Mat(); // 예외 발생 시 빈 행렬 반환
        }
    }
    private List<DMatch> filterMatches(List<KeyPoint> origin_kp, List<KeyPoint> current_kp, List<DMatch> matches) {
        List<DMatch> validMatches = new ArrayList<>();
        for (DMatch match : matches) {
            if (match.queryIdx < origin_kp.size() && match.trainIdx < current_kp.size()) {
                validMatches.add(match);
            }
        }
        return validMatches;
    }

























    private double estimateScaleFactor(List<KeyPoint> keypoints1, List<KeyPoint> keypoints2, List<DMatch> matches) {
        double scaleSum = 0;
        int count = 0;

        List<DMatch> validMatches = filterMatches(keypoints1, keypoints2, matches);

        for (DMatch match : validMatches) {
            Point pt1 = keypoints1.get(match.queryIdx).pt;
            Point pt2 = keypoints2.get(match.trainIdx).pt;

            double distance1 = Math.sqrt(pt1.x * pt1.x + pt1.y * pt1.y);
            double distance2 = Math.sqrt(pt2.x * pt2.x + pt2.y * pt2.y);

            if (distance1 > 0 && distance2 > 0) {
                scaleSum += distance2 / distance1;
                count++;
            }
        }

        return count > 0 ? scaleSum / count : 1.0;
    }

    private void normalizeKeypoints(List<KeyPoint> keypoints, double scaleFactor) {
        for (KeyPoint kp : keypoints) {
            kp.pt.x *= scaleFactor;
            kp.pt.y *= scaleFactor;
        }
    }

    private BufferedImage convertMatToBufferedImage(Mat mat) {
        int type = BufferedImage.TYPE_BYTE_GRAY;
        if (mat.channels() > 1) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }
        BufferedImage image = new BufferedImage(mat.cols(), mat.rows(), type);
        mat.get(0, 0, ((DataBufferByte) image.getRaster().getDataBuffer()).getData());
        return image;
    }


    private void drawMatchesWithBoundingBox(BufferedImage image1, List<KeyPoint> keypoints1,
                                            BufferedImage image2, List<KeyPoint> keypoints2,
                                            List<DMatch> matches, String outputFileName) {
        // Create a new image that combines both images side by side
        int width = image1.getWidth() + image2.getWidth();
        int height = Math.max(image1.getHeight(), image2.getHeight());
        BufferedImage combinedImage = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);

        // Draw the first image
        combinedImage.createGraphics().drawImage(image1, 0, 0, null);

        // Draw the second image next to the first one
        combinedImage.createGraphics().drawImage(image2, image1.getWidth(), 0, null);

        // Prepare graphics for drawing lines between matched keypoints
        Graphics2D g2d = combinedImage.createGraphics();
        g2d.setColor(Color.RED);
        BasicStroke stroke = new BasicStroke(1);
        g2d.setStroke(stroke);

        List<DMatch> validMatches = filterMatches(keypoints1, keypoints2, matches);

        // Draw lines connecting matched keypoints
        for (DMatch match : validMatches) {
            Point pt1 = keypoints1.get(match.queryIdx).pt;
            Point pt2 = keypoints2.get(match.trainIdx).pt;

            // Scale the coordinates of the keypoints from the original image
            int x1 = (int) pt1.x;
            int y1 = (int) pt1.y;
            int x2 = (int) (image1.getWidth() + pt2.x);
            int y2 = (int) pt2.y;

            g2d.drawLine(x1, y1, x2, y2);

            // Draw blue circle at pt1
            g2d.setColor(Color.BLUE);
            g2d.fillOval(x1 - 5, y1 - 5, 10, 10);

            // Draw green circle at pt2
            g2d.setColor(Color.GREEN);
            g2d.fillOval(x2 - 5, y2 - 5, 10, 10);

            // Reset color to red for next line
            g2d.setColor(Color.RED);
        }

        g2d.setColor(Color.YELLOW);
        BasicStroke stroke2 = new BasicStroke(3);
        g2d.setStroke(stroke2);

        // Calculate bounding box for matched keypoints in the second image (camera frame)
        Rectangle boundingBox = calculateBoundingBox(keypoints2, validMatches);
        g2d.drawRect(image1.getWidth() + boundingBox.x, boundingBox.y,
                boundingBox.width, boundingBox.height);

        // Save the combined image to file
        try {
            File outputFile = new File(outputFileName);
            ImageIO.write(combinedImage, "jpg", outputFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Rectangle calculateBoundingBox(List<KeyPoint> keypoints, List<DMatch> matches) {
        // Initialize bounding box coordinates
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        // Iterate through matched keypoints to find extreme coordinates
        for (DMatch match : matches) {
            Point pt = keypoints.get(match.trainIdx).pt;
            int x = (int) pt.x;
            int y = (int) pt.y;

            // Update bounding box coordinates
            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;
        }

        // Calculate width and height of the bounding box
        int width = maxX - minX;
        int height = maxY - minY;

        // Return bounding box as a Rectangle object
        return new Rectangle(minX, minY, width, height);
    }

    private Mat scaleImage(Mat image, double scaleRatio) {
        int newWidth = (int) (image.cols() * scaleRatio);
        int newHeight = (int) (image.rows() * scaleRatio);
        Mat scaledImage = new Mat();
        Imgproc.resize(image, scaledImage, new Size(newWidth, newHeight));
        return scaledImage;
    }

    private Tensor<TFloat32> matToTensor(Mat mat) {
        byte[] data = new byte[mat.rows() * mat.cols() * mat.channels()];
        mat.get(0, 0, data);

        long[] shape = new long[]{1, mat.rows(), mat.cols(), mat.channels()};
        Tensor<TFloat32> tensor = TFloat32.tensorOf(Shape.of(shape));
        tensor.rawData().write(data);

        return tensor;
    }

    private Mat tensorToMat(Tensor<?> tensor){
        long[] shape = tensor.shape().asArray();
        int height = (int) shape[1];
        int width = (int) shape[2];
        int channels = (int) shape[3];

        float[][][][] tensorData = new float[1][height][width][channels];
        tensorData = tensorToJavaArray(tensor);

        Mat mat = new Mat(height, width, CvType.CV_32FC(channels));
        mat = mat.reshape(1, height); // 1-channel, height dimension
        float[] floatData = new float[height * width * channels];

        int idx = 0;
        for (int i = 0; i < height; ++i) {
            for (int j = 0; j < width; ++j) {
                for (int k = 0; k < channels; ++k) {
                    floatData[idx++] = tensorData[0][i][j][k];
                }
            }
        }
        mat.put(0, 0, floatData);

        return mat;
    }

    // TensorFlow Tensor 객체에서 Java 배열로 데이터를 복사하는 메서드
    private float[][][][] tensorToJavaArray(Tensor<?> tensor) {
        long[] shape = tensor.shape().asArray();
        float[][][][] array = new float[1][(int) shape[1]][(int) shape[2]][(int) shape[3]];

        if (tensor.dataType() == TFloat32.DTYPE) {
            FloatDataBuffer floatBuffer = tensor.rawData().asFloats();
            floatBuffer.read(array[0][0][0]);
        } else {
            throw new IllegalArgumentException("Unsupported data type: " + tensor.dataType());
        }

        return array;
    }

    private Mat superResolutionCameraImg(Mat camImg){
        String modelPath = "/home/ubuntu2004/ROBOMI/robomi_backend/resources/model";

        try{
            SavedModelBundle model = SavedModelBundle.load(modelPath, "serve");

            Session session = model.session();

            Tensor<TFloat32> inputTensor = matToTensor(camImg);

            Tensor<?> outputTensor = session.runner()
                    .feed("input", inputTensor)
                    .fetch("output")
                    .run()
                    .get(0);

            Mat outputMat = tensorToMat(outputTensor);

            model.close();
            session.close();

            return outputMat;
        } catch(Exception e){
            System.out.println("Tensor error");
            e.printStackTrace();
            return new Mat();
        }


    }
}
