package com.robomi.entity;

import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "capture")
@Data
public class CaptureEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seq;
    private String name;
    private String imgPath;
    private Long status;
    private String updateDate;
}
