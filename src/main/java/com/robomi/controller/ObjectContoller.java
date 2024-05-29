package com.robomi.controller;

import com.robomi.dto.ObjectDTO;
import com.robomi.service.ObjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
