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
    private final MessageWebSocketHandler msgWebSocketHandler;

    @Autowired
    public WebsocketConfig(VideoWebSocketHandler videoWebSocketHandler, MessageWebSocketHandler msgWebSocketHandler){
        this.videoWebSocketHandler = videoWebSocketHandler;
        this.msgWebSocketHandler = msgWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(videoWebSocketHandler, "/video").setAllowedOrigins("*");
        registry.addHandler(msgWebSocketHandler, "/msg").setAllowedOrigins("*");
        registry.addHandler(new AudioWebSocketHandler(), "/audio").setAllowedOrigins("*");
    }
}
