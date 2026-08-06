package com.forge.knowledge;

import com.forge.knowledge.repository.ConceptPrerequisiteRepository;
import com.forge.knowledge.service.KnowledgeGraphService;
import com.forge.topic.entity.Topic;
import com.forge.topic.repository.TopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeGraphServiceTest {

    @Mock private ConceptPrerequisiteRepository prerequisiteRepository;
    @Mock private TopicRepository topicRepository;

    private KnowledgeGraphService service;

    @BeforeEach
    void setUp() {
        service = new KnowledgeGraphService(prerequisiteRepository, topicRepository);
    }

    @Test
    void matchConceptShouldMatchHyphenatedSlugsToTitledTopics() {
        assertEquals("binary-search", service.matchConcept("Binary Search"));
        assertEquals("dynamic-programming", service.matchConcept("Dynamic Programming"));
        assertEquals("hash-table", service.matchConcept("Hash Table"));
        assertEquals("two-pointers", service.matchConcept("Two Pointers"));
        assertEquals("sliding-window", service.matchConcept("Sliding Window"));
    }

    @Test
    void matchConceptShouldReturnNullForUnknownOrBlankTitles() {
        assertNull(service.matchConcept("Quantum Computing"));
        assertNull(service.matchConcept(null));
        assertNull(service.matchConcept("  "));
    }

    @Test
    void propagateBoostShouldBoostRelatedTopicsByTitle() {
        Topic topic = new Topic();
        topic.setId(UUID.randomUUID());
        topic.setTitle("Binary Search");
        topic.setMastery(50);
        topic.setConfidence(5);
        Page<Topic> page = new PageImpl<>(List.of(topic));

        com.forge.knowledge.entity.ConceptPrerequisite dependent = new com.forge.knowledge.entity.ConceptPrerequisite();
        dependent.setConceptSlug("binary-search");

        when(topicRepository.findByUserId(any(UUID.class), any(PageRequest.class))).thenReturn(page);
        when(prerequisiteRepository.findByConceptSlug("array")).thenReturn(List.of());
        when(prerequisiteRepository.findByPrerequisiteSlug("array")).thenReturn(List.of(dependent));

        int boosted = service.propagateBoost(UUID.randomUUID(), "array", 10);

        assertEquals(1, boosted);
        assertEquals(54, topic.getMastery());
        assertEquals(6, topic.getConfidence());
    }

    @Test
    void matchConceptShouldHandleLowercaseInput() {
        assertEquals("stack", service.matchConcept("stack"));
        assertEquals("queue", service.matchConcept("queue"));
    }
}
