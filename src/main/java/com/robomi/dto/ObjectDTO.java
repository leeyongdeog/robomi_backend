package com.robomi.dto;

import lombok.Data;

@Data
public class ObjectDTO {
    private Long seq;
    private String name;
    private String imgPath;
    private Long display;
    private String updateDate;
    private String createDate;
}
