package com.robomi.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CaptureDTO {
    private Long seq;
    private String name;
    private String img_path;
    private Long status;
    private LocalDateTime update_date;
}
