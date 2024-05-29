package com.robomi.service;

import com.robomi.dto.CaptureDTO;
import com.robomi.entity.CaptureEntity;
import com.robomi.repository.CaptureRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
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

        return dto;
    }
}
