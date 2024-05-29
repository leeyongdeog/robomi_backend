package com.robomi.service;

import com.robomi.entity.ManagerEntity;
import com.robomi.repository.ManagerRepo;
import com.robomi.dto.ManagerDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;

import java.util.List;
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
}
