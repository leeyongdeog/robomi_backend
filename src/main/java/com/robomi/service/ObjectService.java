package com.robomi.service;

import com.robomi.dto.ObjectDTO;
import com.robomi.entity.ManagerEntity;
import com.robomi.entity.ObjectEntity;
import com.robomi.repository.ObjectRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ObjectService {
    @Autowired
    private ObjectRepo objectRepo;

    public List<ObjectDTO> getAllObjects(){
        List<ObjectEntity> objectEntities = objectRepo.findAll();
        return objectEntities.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<ObjectDTO> getObjectByDisplay(Long display){
        List<ObjectEntity> objectEntities = objectRepo.findByObjectByDisplay(display);
        return objectEntities.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    private ObjectDTO convertToDTO(ObjectEntity ent){
        ObjectDTO dto = new ObjectDTO();
        dto.setSeq(ent.getSeq());
        dto.setName(ent.getName());
        dto.setDisplay(ent.getDisplay());
        dto.setImg_path(ent.getImg_path());
        dto.setCreate_date(ent.getCreate_date());
        dto.setUpdate_date(ent.getUpdate_date());

        return dto;
    }

    public String uploadImageToS3(MultipartFile file) throws IOException {
        String keyName = UUID.randomUUID().toString();
        String folderName = "objects";
        String S3BucketName = "robomi-storage";
        String region = "ap-northeast-2";


        S3Uploader.uploadFile(S3BucketName, folderName, keyName, file);

//        https://robomi-storage.s3.ap-northeast-2.amazonaws.com/manager/44b8bdf3-22f0-4aab-8a7e-fc447c9ed86f.jpg
        String imgUrl = "https://" + S3BucketName + ".s3." + region + ".amazonaws.com/" +folderName + "/" + keyName + ".jpg";
        System.out.println(imgUrl);
        return imgUrl;
    }

    public void addObject(String name, String imgUrl){
        ObjectEntity entity = new ObjectEntity();
        System.out.println(name + "/" + imgUrl);
        entity.setName(name);
        entity.setImg_path(imgUrl);
        entity.setDisplay(1l);

        LocalDateTime currentTime = LocalDateTime.now();

        entity.setUpdate_date(currentTime);
        entity.setCreate_date(currentTime);
        System.out.println("/" + entity);
        objectRepo.save(entity);
    }
}
