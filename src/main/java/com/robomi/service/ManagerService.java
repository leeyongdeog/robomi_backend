package com.robomi.service;

import com.robomi.entity.ManagerEntity;
import com.robomi.repository.ManagerRepo;
import com.robomi.dto.ManagerDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ManagerService {
    @Autowired
    private ManagerRepo managerRepo;

    public List<ManagerDTO> getAllManagers(){
        List<ManagerEntity> managers = managerRepo.findAll();
        System.out.println("Managers: " + managers);
        return managers.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<ManagerDTO> getAdminManagers(){
        List<ManagerEntity> managers = managerRepo.findManagerByType(2L);
        return managers.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    private ManagerDTO convertToDTO(ManagerEntity manager){
        ManagerDTO managerDTO = new ManagerDTO();
        managerDTO.setSeq(manager.getSeq());
        managerDTO.setName(manager.getName());
        managerDTO.setType(manager.getType());
        managerDTO.setImgPath(manager.getImgPath());
        managerDTO.setCreateDate(manager.getCreateDate());
        managerDTO.setUpdateDate(manager.getUpdateDate());

        return managerDTO;
    }

    public String uploadImageToS3(MultipartFile file) throws IOException{
        String keyName = UUID.randomUUID().toString();
        String folderName = "manager";
        String S3BucketName = "robomi-storage";


        S3Uploader.uploadFile(S3BucketName, folderName, keyName, file);

        String imgUrl = "s3://" + S3BucketName + "/" + folderName + "/" + keyName + ".jpg";
        System.out.println(imgUrl);
        return imgUrl;
    }

    public void addManager(String name, String imgUrl){
        ManagerEntity entity = new ManagerEntity();
        System.out.println(name + "/" + imgUrl);
        entity.setName(name);
        entity.setImgPath(imgUrl);
        entity.setType(1l);

        LocalDateTime currentTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String strTime = currentTime.format(formatter);

        entity.setCreateDate(strTime);
        entity.setUpdateDate(strTime);

        managerRepo.save(entity);
    }
}
