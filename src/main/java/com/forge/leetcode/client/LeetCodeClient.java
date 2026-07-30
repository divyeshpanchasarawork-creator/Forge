package com.forge.leetcode.client;

import com.forge.leetcode.dto.LeetCodeGraphQlResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class LeetCodeClient {

    private static final String GRAPHQL_URL = "https://leetcode.com/graphql";
    private static final long CACHE_TTL_MS = 5 * 60 * 1000;

    private final RestClient restClient;
    private final Map<String, CachedResponse> cache = new ConcurrentHashMap<>();

    private static final String COMBINED_QUERY = """
            query userCombinedData($username: String!) {
              allQuestionsCount {
                difficulty
                count
              }
              matchedUser(username: $username) {
                submitStatsGlobal {
                  acSubmissionNum {
                    difficulty
                    count
                  }
                }
                problemsSolvedBeatsStats {
                  difficulty
                  percentage
                }
                userCalendar {
                  streak
                  totalActiveDays
                  submissionCalendar
                }
                tagProblemCounts {
                  advanced {
                    tagName
                    tagSlug
                    problemsSolved
                  }
                  intermediate {
                    tagName
                    tagSlug
                    problemsSolved
                  }
                  fundamental {
                    tagName
                    tagSlug
                    problemsSolved
                  }
                }
                languageProblemCount {
                  languageName
                  problemsSolved
                }
              }
              userContestRanking(username: $username) {
                attendedContestsCount
                rating
                globalRanking
                topPercentage
              }
            }
            """;

    public LeetCodeClient() {
        this.restClient = RestClient.builder()
                .baseUrl(GRAPHQL_URL)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Referer", "https://leetcode.com/")
                .defaultHeader("Origin", "https://leetcode.com")
                .defaultHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                .build();
    }

    public LeetCodeGraphQlResponse fetchUserProfile(String username) {
        CachedResponse cached = cache.get(username);
        if (cached != null && System.currentTimeMillis() - cached.timestamp < CACHE_TTL_MS) {
            log.debug("Returning cached LeetCode data for user: {}", username);
            return cached.response;
        }

        log.info("Fetching LeetCode profile for: {}", username);
        try {
            LeetCodeGraphQlResponse response = restClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "query", COMBINED_QUERY,
                            "variables", Map.of("username", username),
                            "operationName", "userCombinedData"
                    ))
                    .retrieve()
                    .body(LeetCodeGraphQlResponse.class);

            if (response != null && response.getData() != null && response.getData().getMatchedUser() != null) {
                cache.put(username, new CachedResponse(response, System.currentTimeMillis()));
                return response;
            }

            log.warn("No data returned from LeetCode for user: {}", username);
            return null;
        } catch (Exception e) {
            log.error("Failed to fetch LeetCode profile for {}: {}", username, e.getMessage());
            return null;
        }
    }

    private record CachedResponse(LeetCodeGraphQlResponse response, long timestamp) {}
}
