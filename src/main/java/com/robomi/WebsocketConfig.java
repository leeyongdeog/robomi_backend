package com.robomi;

import com.robomi.store.VideoDataStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;

@Configuration
@EnableWebSocket
public class WebsocketConfig implements WebSocketConfigurer {
    private final VideoWebSocketHandler videoWebSocketHandler;

    @Autowired
    public WebsocketConfig(VideoWebSocketHandler videoWebSocketHandler){
        System.out.println("-----------------WebsocketConfig constructor");
        this.videoWebSocketHandler = videoWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(videoWebSocketHandler, "/video").setAllowedOrigins("*");
        registry.addHandler(new AudioWebSocketHandler(), "/audio").setAllowedOrigins("*");
    }
}
