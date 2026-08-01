package com.forge.search;

import com.forge.common.util.ProblemLoader;
import com.forge.search.dto.ProblemSearchItem;
import com.forge.search.service.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SearchServiceTest {

    @Mock private ProblemLoader problemLoader;

    private SearchService searchService;

    @BeforeEach
    void setUp() {
        ProblemLoader.ProblemEntry twoSum = new ProblemLoader.ProblemEntry("Two Sum", "two-sum", "Easy");
        ProblemLoader.ProblemEntry validParentheses = new ProblemLoader.ProblemEntry("Valid Parentheses", "valid-parentheses", "Easy");
        ProblemLoader.ProblemEntry minSubarraySum = new ProblemLoader.ProblemEntry("Minimum Size Subarray Sum", "minimum-size-subarray-sum", "Medium");

        when(problemLoader.getAllProblems()).thenReturn(List.of(twoSum, validParentheses, minSubarraySum));
        when(problemLoader.getAllTagSlugs()).thenReturn(Set.of("hash-table", "stack", "prefix-sum"));
        when(problemLoader.getProblemsForTag("hash-table")).thenReturn(List.of(twoSum));
        when(problemLoader.getProblemsForTag("stack")).thenReturn(List.of(validParentheses));
        when(problemLoader.getProblemsForTag("prefix-sum")).thenReturn(List.of(minSubarraySum));

        searchService = new SearchService(problemLoader);
    }

    @Test
    void searchIsCaseInsensitive() {
        List<ProblemSearchItem> results = searchService.searchProblems("two sum");
        assertEquals(1, results.size());
        assertEquals("Two Sum", results.get(0).title());
        assertEquals("two-sum", results.get(0).titleSlug());
    }

    @Test
    void searchMatchesMixedCase() {
        List<ProblemSearchItem> results = searchService.searchProblems("VALID PARENTHESES");
        assertEquals(1, results.size());
        assertEquals("Valid Parentheses", results.get(0).title());
    }

    @Test
    void searchRanksWordStartsAboveSubstring() {
        List<ProblemSearchItem> results = searchService.searchProblems("t");
        assertFalse(results.isEmpty());
        assertEquals("Two Sum", results.get(0).title());
        assertEquals(2, results.size());
        assertEquals("Valid Parentheses", results.get(1).title());
    }

    @Test
    void searchFindsProblemsByMatchingTag() {
        List<ProblemSearchItem> results = searchService.searchProblems("hash");
        assertFalse(results.isEmpty());
        assertEquals("Two Sum", results.get(0).title());
    }

    @Test
    void searchReturnsEmptyForBlankQuery() {
        assertTrue(searchService.searchProblems("   ").isEmpty());
    }

    @Test
    void searchReturnsEmptyWhenNothingMatches() {
        assertTrue(searchService.searchProblems("zzz-not-a-problem").isEmpty());
    }
}
