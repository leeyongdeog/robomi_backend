package com.robomi.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "objects")
@Data
public class ObjectEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seq;
    private String name;
    private String img_path;
    private Long display;
    private LocalDateTime update_date;
    private LocalDateTime create_date;
}
