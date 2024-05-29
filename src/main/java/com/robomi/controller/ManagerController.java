package com.robomi.controller;

import com.robomi.dto.ManagerDTO;
import com.robomi.service.ManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
