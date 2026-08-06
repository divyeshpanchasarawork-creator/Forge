package com.forge.topic;

import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
import com.forge.common.util.SecurityUtils;
import com.forge.security.UserPrincipal;
import com.forge.topic.dto.TopicRequest;
import com.forge.topic.dto.TopicResponse;
import com.forge.topic.entity.Topic;
import com.forge.topic.mapper.TopicMapper;
import com.forge.topic.repository.TopicRepository;
import com.forge.topic.service.TopicService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TopicServiceTest {

    @Mock private TopicRepository topicRepository;
    @Mock private UserRepository userRepository;

    private TopicService service;
    private UUID userId;
    private UUID topicId;

    @BeforeEach
    void setUp() {
        service = new TopicService(topicRepository, userRepository, new TopicMapper());
        userId = UUID.randomUUID();
        topicId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(userId, "testuser", "password");
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateTopicPreservesFieldsNotSentInRequest() {
        User user = new User();
        user.setId(userId);

        Topic topic = new Topic();
        topic.setId(topicId);
        topic.setUser(user);
        topic.setTitle("Old Title");
        topic.setDescription("original description");
        topic.setCategory("ARRAY");
        topic.setConfidence(7);
        topic.setMastery(80);
        topic.setNotes("original notes");

        when(topicRepository.findById(topicId)).thenReturn(Optional.of(topic));
        when(topicRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TopicRequest request = new TopicRequest();
        request.setTitle("New Title");
        request.setCategory("DYNAMIC_PROGRAMMING");

        TopicResponse response = service.updateTopic(topicId, request);

        assertEquals("New Title", response.getTitle());
        assertEquals("DYNAMIC_PROGRAMMING", response.getCategory());
        assertEquals("original description", response.getDescription());
        assertEquals("original notes", response.getNotes());
        assertEquals(7, response.getConfidence());
        assertEquals(80, response.getMastery());

        ArgumentCaptor<Topic> captor = ArgumentCaptor.forClass(Topic.class);
        verify(topicRepository).save(captor.capture());
        assertEquals("original description", captor.getValue().getDescription());
        assertEquals("original notes", captor.getValue().getNotes());
    }
}
