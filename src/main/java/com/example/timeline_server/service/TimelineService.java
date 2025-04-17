package com.example.timeline_server.service;

import com.example.timeline_server.dto.FeedInfo;
import com.example.timeline_server.dto.SocialPost;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class TimelineService {

    private final FeedStore feedStore;
    private final FollowerStore followerStore;

    public TimelineService(FeedStore feedStore, FollowerStore followerStore) {
        this.feedStore = feedStore;
        this.followerStore = followerStore;
    }

    // userId 기반 피드 조회
    public List<SocialPost> listUserFeed(String userId) {
        // 피드 조회, userId 기반
        List<FeedInfo> feedList = feedStore.listFeed(userId);

        // 좋아요 수 측정 <postId, Count>
        Map<Integer, Long> likes = feedStore.countLikes(
                feedList.stream()
                        .map(FeedInfo::getFeedId)
                        .toList());

        return feedList.stream().map(
                post -> new SocialPost(post, likes.getOrDefault(post.getFeedId(), 0L))).toList();
    }

    public List<SocialPost> getRandomPost(String userId, double randomPost) {
        List<SocialPost> myPost = userId.equals("none") ? List.of() : listMyFeed(userId);
        int randomPostSize = Math.max(10, (int) Math.ceil(myPost.size() * randomPost));
        List<SocialPost> allPost = new ArrayList<>(listAllFeed());

        Set<Integer> myPostIds = myPost.stream()
                .map(SocialPost::getFeedId)
                .collect(Collectors.toSet());

        allPost.removeIf(post -> myPostIds.contains(post.getFeedId()));

        List<SocialPost> picked;
        if (randomPostSize >= allPost.size()) {
            picked = allPost;
        } else {
            Collections.shuffle(allPost);
            picked = new ArrayList<>(allPost.subList(0, randomPostSize));
        }

        allPost.removeIf(post -> myPostIds.contains(post.getFeedId()));

        return Stream.concat(myPost.stream(), picked.stream())
                .sorted(Comparator.comparing(SocialPost::getUploadDatetime).reversed())
                .collect(Collectors.toList());
    }

    public List<SocialPost> listFollowerFeed(Set<String> followerSet) {
        return followerSet
                .stream()
                .map(this::listUserFeed)
                .filter(Objects::nonNull) // Filter out any nulls that might have resulted from invalid ids
                .flatMap(List::stream) // Flatten the stream of lists into a stream of SocialPost
                .collect(Collectors.toList());
    }

    // 내 피드 조회
    public List<SocialPost> listMyFeed(String userId) {
        // 팔로우들 조회, Redis Set
        Set<String> followers = followerStore.listFollower(String.valueOf(userId));

        // userId 기반 피드 조회, 본인 ID 조회
        List<SocialPost> myPost = listUserFeed(userId);
        // 팔로워들 피드 조회
        List<SocialPost> followerFeed = listFollowerFeed(followers);

        return Stream.concat(myPost.stream(), followerFeed.stream())
                .sorted(Comparator.comparing(SocialPost::getUploadDatetime).reversed())
                .collect(Collectors.toList());
    }

    public List<SocialPost> listAllFeed() {
        // Sorted Set 기반의 전체 조회
        List<FeedInfo> feedList = feedStore.allFeed();

        Map<Integer, Long> likes = feedStore.countLikes(
                feedList.stream()
                        .map(FeedInfo::getFeedId)
                        .toList());

        return feedList.stream().map(
                post -> new SocialPost(post, likes.getOrDefault(post.getFeedId(), 0L))).toList();
    }

    public boolean likePost(int userId, int postId) {
        if (feedStore.isLikePost(userId, postId)) {
            feedStore.unlikePost(userId, postId);
            return false;
        } else {
            feedStore.likePost(userId, postId);
            return true;
        }
    }

    public Long countLike(int postId) {
        return feedStore.countLikes(postId);
    }
}
