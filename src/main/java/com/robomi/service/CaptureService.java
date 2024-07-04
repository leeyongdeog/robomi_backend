package com.robomi.service;

import com.robomi.dto.CaptureDTO;
import com.robomi.entity.CaptureEntity;
import com.robomi.entity.ManagerEntity;
import com.robomi.repository.CaptureRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CaptureService {
    @Autowired
    private CaptureRepo captureRepo;

    public List<CaptureDTO> getAllCaptures(){
        List<CaptureEntity> entities = captureRepo.findAll();
        return entities.stream().map(this::convertToDTO).collect(Collectors.toList());
    }
    public List<CaptureDTO> getWarningCaptures(){
        List<CaptureEntity> entities = captureRepo.findWarningCaptures();
        return entities.stream().map(this::convertToDTO).collect(Collectors.toList());
    }
    public List<CaptureDTO> getCapturesByName(String name){
        List<CaptureEntity> entities = captureRepo.findCapturesByName(name);
        return entities.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    private CaptureDTO convertToDTO(CaptureEntity ent){
        CaptureDTO dto = new CaptureDTO();
        dto.setSeq(ent.getSeq());
        dto.setName(ent.getName());
        dto.setStatus(ent.getStatus());
        dto.setImg_path(ent.getImg_path());
        dto.setUpdate_date(ent.getUpdate_date());

        return dto;
    }

    public String uploadImageToS3(MultipartFile file) throws IOException {
        String keyName = UUID.randomUUID().toString();
        String folderName = "captures";
        String S3BucketName = "robomi-storage";
        String region = "ap-northeast-2";

        S3Uploader.uploadFile(S3BucketName, folderName, keyName, file);

        String imgUrl = "https://" + S3BucketName + ".s3." + region + ".amazonaws.com/" +folderName + "/" + keyName + ".jpg";
        System.out.println(imgUrl);
        return imgUrl;
    }

    public void addCapture(String name, String imgUrl, Long status){
        CaptureEntity entity = new CaptureEntity();
        System.out.println(name + "/" + imgUrl);
        entity.setName(name);
        entity.setImg_path(imgUrl);
        entity.setStatus(status);

        LocalDateTime currentTime = LocalDateTime.now();

        entity.setUpdate_date(currentTime);
        System.out.println("/" + entity);
        captureRepo.save(entity);
    }
}
