package com.robomi.entity;

import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "manager")
@Data
public class ManagerEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seq;
    private String name;
    private String imgPath;
    private String createDate;
    private String updateDate;
    private Long type;
}
