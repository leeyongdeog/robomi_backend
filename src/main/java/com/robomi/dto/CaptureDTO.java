package com.robomi.dto;

import lombok.Data;

@Data
public class CaptureDTO {
    private Long seq;
    private String name;
    private String imgPath;
    private Long status;
    private String updateDate;
}
