package com.example.timeline_server.service;

import com.example.timeline_server.dto.FollowMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class FollowListener {

    private final ObjectMapper objectMapper;
    private final FollowerStore followStore;

    public FollowListener(ObjectMapper objectMapper, FollowerStore followStore) {
        this.objectMapper = objectMapper;
        this.followStore = followStore;
    }

    @KafkaListener(topics = "user.follower", groupId = "timeline-server")
    public void listen(String message) {
        try {
            FollowMessage followMessage = objectMapper.readValue(message, FollowMessage.class);
            followStore.followUser(followMessage);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }
}
