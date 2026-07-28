package com.forge.topic.controller;

import com.forge.common.dto.ApiResponse;
import com.forge.common.dto.PagedResponse;
import com.forge.topic.dto.TopicRequest;
import com.forge.topic.dto.TopicResponse;
import com.forge.topic.service.TopicService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/topics")
@RequiredArgsConstructor
public class TopicController {

    private final TopicService topicService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<TopicResponse>>> getTopics(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.success(topicService.getTopics(page, size, category, status)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TopicResponse>> getTopic(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(topicService.getTopicById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TopicResponse>> createTopic(@Valid @RequestBody TopicRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Topic created", topicService.createTopic(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TopicResponse>> updateTopic(@PathVariable UUID id, @Valid @RequestBody TopicRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Topic updated", topicService.updateTopic(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTopic(@PathVariable UUID id) {
        topicService.deleteTopic(id);
        return ResponseEntity.ok(ApiResponse.success("Topic deleted", null));
    }

    @GetMapping("/weak")
    public ResponseEntity<ApiResponse<List<TopicResponse>>> getWeakTopics() {
        return ResponseEntity.ok(ApiResponse.success(topicService.getWeakTopics()));
    }

    @GetMapping("/strong")
    public ResponseEntity<ApiResponse<List<TopicResponse>>> getStrongTopics() {
        return ResponseEntity.ok(ApiResponse.success(topicService.getStrongTopics()));
    }

    @GetMapping("/revision-needed")
    public ResponseEntity<ApiResponse<List<TopicResponse>>> getTopicsNeedingRevision() {
        return ResponseEntity.ok(ApiResponse.success(topicService.getTopicsNeedingRevision()));
    }
}
