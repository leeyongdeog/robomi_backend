package com.robomi;

import com.robomi.object.DetectingObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Configure {
    @Bean
    public DetectingObject detectingObject(){
        return DetectingObject.getInstance();
    }
}
