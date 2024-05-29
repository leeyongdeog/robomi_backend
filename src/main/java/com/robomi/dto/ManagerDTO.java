package com.robomi.dto;

import lombok.Data;

@Data
public class ManagerDTO {
    private Long seq;
    private String name;
    private String imgPath;
    private String createDate;
    private String updateDate;
    private Long type;
}
