package com.forge.leetcode;

import com.forge.auth.entity.User;
import com.forge.leetcode.dto.LeetCodeGraphQlResponse;
import com.forge.leetcode.service.LeetCodeTopicMapper;
import com.forge.topic.entity.Topic;
import com.forge.topic.repository.TopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class LeetCodeTopicMapperTest {

    @Mock private TopicRepository topicRepository;

    private LeetCodeTopicMapper mapper;
    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        mapper = new LeetCodeTopicMapper(topicRepository);
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
    }

    private LeetCodeGraphQlResponse.TagCount tag(String name, String slug, int solved) {
        LeetCodeGraphQlResponse.TagCount t = new LeetCodeGraphQlResponse.TagCount();
        t.setTagName(name);
        t.setTagSlug(slug);
        t.setProblemsSolved(solved);
        return t;
    }

    private Topic topic(String title) {
        Topic t = new Topic();
        t.setId(UUID.randomUUID());
        t.setTitle(title);
        t.setSource("LEETCODE");
        return t;
    }

    @Test
    void shouldDeleteStaleTopicsWhenTagDropsOff() {
        Topic stale = topic("Old Tag");
        when(topicRepository.findByUserIdAndSource(userId, "LEETCODE")).thenReturn(List.of(stale));

        List<Topic> result = mapper.mapToTopics(user, List.of(tag("Array", "array", 10)), "mixed");

        ArgumentCaptor<Iterable<Topic>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(topicRepository).deleteAll(captor.capture());
        assertTrue(captor.getValue().iterator().hasNext());
        assertEquals(stale, captor.getValue().iterator().next());
        assertEquals(1, result.size());
        assertEquals("Array", result.get(0).getTitle());
    }

    @Test
    void shouldNotDeleteWhenAllExistingTopicsStillPresent() {
        Topic existing = topic("Array");
        when(topicRepository.findByUserIdAndSource(userId, "LEETCODE")).thenReturn(List.of(existing));

        List<Topic> result = mapper.mapToTopics(user, List.of(tag("Array", "array", 10)), "mixed");

        verify(topicRepository, never()).deleteAll(any());
        assertEquals(1, result.size());
    }
}
