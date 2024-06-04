package com.robomi.entity;

import lombok.Data;
import java.time.LocalDateTime;
import javax.persistence.*;

@Entity
@Table(name = "manager")
@Data
public class ManagerEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seq;
    private String name;
    private String img_path;
    private LocalDateTime create_date;
    private LocalDateTime update_date;
    private Long type;
}
