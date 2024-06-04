package com.robomi.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ObjectDTO {
    private Long seq;
    private String name;
    private String img_path;
    private Long display;
    private LocalDateTime update_date;
    private LocalDateTime create_date;
}
