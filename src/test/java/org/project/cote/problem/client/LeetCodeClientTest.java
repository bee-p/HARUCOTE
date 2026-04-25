package org.project.cote.problem.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.cote.problem.client.LeetCodeClient.QuestionDetail;
import org.project.cote.problem.client.LeetCodeClient.QuestionSummary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeetCodeClientTest {

    private LeetCodeClient leetCodeClient;

    @BeforeEach
    void setUp() {
        RestClient restClient = RestClient.builder()
                .baseUrl("https://leetcode.com")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.USER_AGENT,
                        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                .defaultHeader("Referer", "https://leetcode.com/problemset/")
                .build();

        leetCodeClient = new LeetCodeClient(restClient);
    }

    @Test
    @DisplayName("EASY 난이도 문제 목록을 LeetCode에서 가져온다")
    void fetchProblemList_easy() {
        List<QuestionSummary> problems = leetCodeClient.fetchProblemList("EASY", 5);

        System.out.println("=== EASY 문제 목록 ===");
        problems.forEach(p -> System.out.printf("- [%s] %s (%s)%n",
                p.difficulty(), p.title(), p.titleSlug()));

        assertNotNull(problems);
        assertEquals(5, problems.size());
        problems.forEach(p -> {
            assertNotNull(p.title());
            assertFalse(p.title().isBlank());
            assertNotNull(p.titleSlug());
            assertFalse(p.titleSlug().isBlank());
            assertTrue("Easy".equalsIgnoreCase(p.difficulty()));
        });
    }

    @Test
    @DisplayName("titleSlug으로 문제 상세를 가져온다")
    void fetchProblemDetail_twoSum() {
        QuestionDetail detail = leetCodeClient.fetchProblemDetail("two-sum");

        System.out.println("=== two-sum 상세 ===");
        System.out.println("questionId: " + detail.questionId());
        System.out.println("title: " + detail.title());
        System.out.println("difficulty: " + detail.difficulty());
        System.out.println("topicTags: " + detail.topicTags());
        System.out.println("exampleTestcases: " + detail.exampleTestcases());
        System.out.println("hints: " + detail.hints());
        System.out.println("content (앞 200자): "
                + detail.content().substring(0, Math.min(200, detail.content().length())));

        assertEquals("Two Sum", detail.title());
        assertEquals("two-sum", detail.titleSlug());
        assertTrue(detail.content().contains("nums"));
        assertTrue(detail.content().contains("target"));
        assertNotNull(detail.topicTags());
        assertFalse(detail.topicTags().isEmpty());
        assertNotNull(detail.exampleTestcases());
        assertFalse(detail.exampleTestcases().isBlank());
    }
}
