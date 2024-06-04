package com.robomi.controller;

import com.robomi.dto.CaptureDTO;
import com.robomi.service.CaptureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/capture")
public class CaptureController {
    @Autowired
    private CaptureService captureService;

    @GetMapping("/allCaptures")
    public List<CaptureDTO> getAllCaptures(){
        return captureService.getAllCaptures();
    }

    @GetMapping("/warningCaptures")
    public List<CaptureDTO> getWarningCaptures(){
        return captureService.getWarningCaptures();
    }

    @GetMapping("/capturesByName/{name}")
    public List<CaptureDTO> getCapturesByName(@PathVariable String name){
        return captureService.getCapturesByName(name);
    }
}
