package com.robomi.controller;

import com.robomi.dto.ManagerDTO;
import com.robomi.service.ManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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

    @PostMapping("/addManager")
    public void addManager(@RequestParam("img")MultipartFile file, @RequestParam("name") String name) throws IOException{
        if(file.isEmpty()){
            throw new IllegalArgumentException("File is Empty.");
        }

        String imgUrl = managerService.uploadImageToS3(file);
        System.out.println("------------"+name + "/" + imgUrl);
        managerService.addManager(name, imgUrl);
    }
}
