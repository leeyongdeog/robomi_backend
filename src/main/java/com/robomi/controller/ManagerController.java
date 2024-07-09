package com.robomi.controller;

import com.robomi.dto.ManagerDTO;
import com.robomi.service.ManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/manager")
public class ManagerController {
    @Autowired
    private ManagerService managerService;

    @GetMapping("/allManagers")
    public List<ManagerDTO> getAllManagers(){
        return managerService.getAllManagers();
    }

    @GetMapping("/adminManagers")
    public List<ManagerDTO> getAdminManagers(){
        return managerService.getAdminManagers();
    }

    @GetMapping("/checkFace")
    public int checkFace(@RequestParam("img")MultipartFile file){
        int ret = 0;
        try{
            ret = managerService.checkFace(file);
        }
        catch(IOException e){
            System.out.println("Face checking error");
        }
        return ret;
    }

    @PostMapping("/addManager")
    public void addManager(@RequestParam("img")MultipartFile file, @RequestParam("name") String name) throws IOException{
        if(file.isEmpty()){
            throw new IllegalArgumentException("File is Empty.");
        }

        MultipartFile resized = saveResizedImage(file);
        String imgUrl = managerService.uploadImageToS3(resized);
        System.out.println("------------"+name + "/" + imgUrl);
        managerService.addManager(name, imgUrl);
    }

    public MultipartFile saveResizedImage(MultipartFile file) throws IOException {
        String uploadDir = "temp_imagefolder";
        // 원본 파일명
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());

        // 저장할 디렉토리 경로
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 원본 이미지를 BufferedImage로 읽어옴
        BufferedImage originalImage = ImageIO.read(file.getInputStream());
        BufferedImage rotatedImage = rotateImage(originalImage, -90);

        // 세로 사이즈를 350픽셀로 맞추는 비율 계산
        double ratio = 350.0 / rotatedImage.getHeight();
        int newWidth = (int) (rotatedImage.getWidth() * ratio);
        int newHeight = 350;

        // 리사이징된 이미지 생성
        BufferedImage resizedImage = resizeImage(rotatedImage, newWidth, newHeight);
        MultipartFile resizedFile = bufferedImageToMultipartFile(resizedImage, file.getOriginalFilename());


        return resizedFile;
    }

    // 이미지를 주어진 각도로 회전하는 메서드
    private BufferedImage rotateImage(BufferedImage originalImage, double angleDegrees) {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        // 회전을 위한 AffineTransform 생성
        AffineTransform transform = new AffineTransform();
        transform.rotate(Math.toRadians(angleDegrees), width / 2.0, height / 2.0);

        // 회전된 이미지 생성
        BufferedImage rotatedImage = new BufferedImage(height, width, originalImage.getType());
        Graphics2D g2d = rotatedImage.createGraphics();
        g2d.setTransform(transform);
        g2d.drawImage(originalImage, 0, 0, null);
        g2d.dispose();

        return rotatedImage;
    }
    // BufferedImage를 원하는 사이즈로 리사이징하는 메서드
    private BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = resizedImage.createGraphics();
        graphics2D.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        graphics2D.dispose();
        return resizedImage;
    }
    private MultipartFile bufferedImageToMultipartFile(BufferedImage image, String originalFilename) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        baos.flush();
        byte[] imageInByte = baos.toByteArray();
        baos.close();
        return new MockMultipartFile(originalFilename, originalFilename, "image/jpeg", new ByteArrayInputStream(imageInByte));
    }
}
