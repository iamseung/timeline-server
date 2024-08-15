package com.example.timeline_server.service;

import com.example.timeline_server.dto.FeedInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class FeedStore {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public FeedStore(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public void savePost(FeedInfo post) {
        try {
            redis.opsForZSet().add("feed:" + post.getUploaderId(),
                    objectMapper.writeValueAsString(post),
                    post.getUploadDatetime().toEpochSecond());

            // 전체 피드
            redis.opsForZSet().add("feed:all",
                    objectMapper.writeValueAsString(post),
                    post.getUploadDatetime().toEpochSecond());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    // 피드 전체 조회
    public List<FeedInfo> allFeed() {
        // 역순 정렬
        Set<String> savedFeed = redis.opsForZSet().reverseRange("feed:all", 0, -1);

        return savedFeed.stream().map(feed -> {
            try {
                return objectMapper.readValue(feed, FeedInfo.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }).toList();
    }

    // 피드 조회, userId 기반
    public List<FeedInfo> listFeed(String userId) {
        // 역순 정렬
        Set<String> savedFeed = redis.opsForZSet().reverseRange("feed:" + userId, 0, -1);

        return savedFeed.stream().map(feed -> {
            try {
                return objectMapper.readValue(feed, FeedInfo.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }).toList();
    }

    public Long likePost(int userId, int postId) {
        return redis.opsForSet().add("likes:" + postId, String.valueOf(userId));
    }

    public Long unlikePost(int userId, int postId) {
        return redis.opsForSet().remove("likes:" + postId, String.valueOf(userId));
    }

    public Boolean isLikePost(int userId, int postId) {
        return redis.opsForSet().isMember("likes:"+postId, String.valueOf(userId));
    }

    public Long countLikes(int postId) {
        return redis.opsForSet().size("likes:" + postId);
    }

    public Map<Integer, Long> countLikes(List<Integer> postIds) {
        Map<Integer, Long> likesMap = new HashMap<>();

        List<Object> results = redis.executePipelined((RedisCallback<Object>) connection -> {
            StringRedisConnection stringRedisConn = (StringRedisConnection) connection;

            for (int postId : postIds) {
                stringRedisConn.sCard("likes:" + postId);
            }
            return null;
        });

        int index = 0;
        for (int postId : postIds) {
            Long likeCount = (Long) results.get(index++); // Get the result from the results list
            likesMap.put(postId, likeCount);
        }

        return likesMap;
    }
}