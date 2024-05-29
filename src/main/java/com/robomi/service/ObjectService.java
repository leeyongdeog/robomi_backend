package com.robomi.service;

import com.robomi.dto.ObjectDTO;
import com.robomi.entity.ObjectEntity;
import com.robomi.repository.ObjectRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
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
        dto.setImgPath(ent.getImgPath());
        dto.setCreateDate(ent.getCreateDate());
        dto.setUpdateDate(ent.getUpdateDate());

        return dto;
    }
}
