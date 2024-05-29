package com.robomi.entity;

import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "objects")
@Data
public class ObjectEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seq;
    private String name;
    private String imgPath;
    private Long display;
    private String updateDate;
    private String createDate;
}
