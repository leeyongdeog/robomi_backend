package com.robomi.dto;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ManagerDTO {
    private Long seq;
    private String name;
    private String img_path;
    private LocalDateTime create_date;
    private LocalDateTime update_date;
    private Long type;
}
