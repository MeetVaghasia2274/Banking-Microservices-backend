package com.finance.notificationservice.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.finance.notificationservice.model.Notification;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    // Map to keep track of user-id to WebSocketSession
    private final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public NotificationWebSocketHandler() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // We will expect the client to send a tiny JSON payload with their user ID upon connecting
        // or we can extract it from the URI if we configure the endpoint like /ws/notifications/{userId}
        // Let's use the URI path to get userId. E.g. ws://localhost:8084/ws/notifications?userId=1
        
        String query = session.getUri().getQuery();
        if (query != null && query.contains("userId=")) {
            try {
                String userIdStr = query.split("userId=")[1].split("&")[0];
                Long userId = Long.parseLong(userIdStr);
                userSessions.put(userId, session);
                System.out.println("WebSocket connected for userId: " + userId);
            } catch (Exception e) {
                session.close(CloseStatus.BAD_DATA);
            }
        } else {
            session.close(CloseStatus.BAD_DATA);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        userSessions.values().remove(session);
        System.out.println("WebSocket disconnected.");
    }

    public void sendNotification(Long userId, Notification notification) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                String json = objectMapper.writeValueAsString(notification);
                session.sendMessage(new TextMessage(json));
            } catch (IOException e) {
                System.err.println("Failed to send WebSocket message: " + e.getMessage());
            }
        }
    }
}
