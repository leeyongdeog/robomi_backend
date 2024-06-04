package com.robomi.entity;

import lombok.Data;
import java.time.LocalDateTime;
import javax.persistence.*;

@Entity
@Table(name = "capture")
@Data
public class CaptureEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seq;
    private String name;
    private String img_path;
    private Long status;
    private LocalDateTime update_date;
}
