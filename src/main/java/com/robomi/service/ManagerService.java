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
        managerDTO.setImg_path(manager.getImg_path());
        managerDTO.setCreate_date(manager.getCreate_date());
        managerDTO.setUpdate_date(manager.getUpdate_date());
        return managerDTO;
    }

    public String uploadImageToS3(MultipartFile file) throws IOException{
        String keyName = UUID.randomUUID().toString();
        String folderName = "manager";
        String S3BucketName = "robomi-storage";
        String region = "ap-northeast-2";


        S3Uploader.uploadFile(S3BucketName, folderName, keyName, file);

//        https://robomi-storage.s3.ap-northeast-2.amazonaws.com/manager/44b8bdf3-22f0-4aab-8a7e-fc447c9ed86f.jpg
        String imgUrl = "https://" + S3BucketName + ".s3." + region + ".amazonaws.com/" +folderName + "/" + keyName + ".jpg";
        System.out.println(imgUrl);
        return imgUrl;
    }

    public void addManager(String name, String imgUrl){
        ManagerEntity entity = new ManagerEntity();
        System.out.println(name + "/" + imgUrl);
        entity.setName(name);
        entity.setImg_path(imgUrl);
        entity.setType(1l);

        LocalDateTime currentTime = LocalDateTime.now();

        entity.setUpdate_date(currentTime);
        entity.setCreate_date(currentTime);
        System.out.println("/" + entity);
        managerRepo.save(entity);
    }
}
