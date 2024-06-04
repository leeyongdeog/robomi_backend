package com.robomi.controller;

import com.robomi.dto.ObjectDTO;
import com.robomi.service.ObjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/object")
public class ObjectContoller {
    @Autowired
    private ObjectService objectService;

    @GetMapping("/allObjects")
    public List<ObjectDTO> getAllObjects(){
        return objectService.getAllObjects();
    }

    @GetMapping("/objectByDisplay/{display}")
    public List<ObjectDTO> getObjectsByDisplay(@PathVariable Long display){
        return objectService.getObjectByDisplay(display);
    }

    @PostMapping("/addObject")
    public void addObject(@RequestParam("img")MultipartFile file, @RequestParam("name") String name) throws IOException{
        if(file.isEmpty()){
            throw new IllegalArgumentException("File is Empty.");
        }

        String imgUrl = objectService.uploadImageToS3(file);
        System.out.println("------------"+name + "/" + imgUrl);
        objectService.addObject(name, imgUrl);
    }
}
