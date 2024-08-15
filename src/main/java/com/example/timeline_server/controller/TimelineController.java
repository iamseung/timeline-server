package com.example.timeline_server.controller;

import com.example.timeline_server.dto.LikeResponse;
import com.example.timeline_server.dto.SocialPost;
import com.example.timeline_server.service.TimelineService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/timeline")
public class TimelineController {

    private final TimelineService timelineService;

    public TimelineController(TimelineService timelineService) {
        this.timelineService = timelineService;
    }

    @GetMapping
    public List<SocialPost> listAllFeed() {
        return timelineService.listAllFeed();
    }

    @GetMapping("/{userId}")
    public List<SocialPost> listFeed(@PathVariable("userId") String userId,
                                     @RequestParam(value = "followingFeed", required = false) boolean includeFollowingFeed,
                                     @RequestParam(value = "randomPost", required = false, defaultValue = "0") double randomPost) {
        if (randomPost > 0) {
            return timelineService.getRandomPost(userId, randomPost);
        }

        return includeFollowingFeed ? timelineService.listMyFeed(userId) : timelineService.listUserFeed(userId);
    }

    // 좋아요 기능
    @GetMapping("/like/{postId}/{userId}")
    public LikeResponse likePost(@PathVariable("postId") int postId, @PathVariable("userId") int userId) {
        boolean isLike = timelineService.likePost(userId, postId);
        Long count = timelineService.countLike(postId);
        return new LikeResponse(count, isLike);
    }

}
