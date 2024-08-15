package com.example.timeline_server.service;

import com.example.timeline_server.dto.FeedInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class FeedListener {

    private final ObjectMapper objectMapper;
    private final FeedStore feedStore;

    public FeedListener(ObjectMapper objectMapper, FeedStore feedStore) {
        this.objectMapper = objectMapper;
        this.feedStore = feedStore;
    }

    // 새로운 피드들이 올라온 경우
    // groupId 를 지정하여 한 서버에서 맡아서 처리하게 설정
    @KafkaListener(topics = "feed.created", groupId = "timeline-server")
    public void listen(String message) {
        try {
            FeedInfo feed = objectMapper.readValue(message, FeedInfo.class);
            feedStore.savePost(feed);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
