package org.project.cote.problem.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.cote.common.dto.ErrorCode;
import org.project.cote.common.exception.ApiException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class LeetCodeClient {

    private final RestClient leetCodeRestClient;

    private static final String PROBLEM_LIST_QUERY = """
            query problemsetQuestionList($categorySlug: String, $limit: Int, $skip: Int, $filters: QuestionListFilterInput) {
              problemsetQuestionList: questionList(
                categorySlug: $categorySlug
                limit: $limit
                skip: $skip
                filters: $filters
              ) {
                total: totalNum
                questions: data {
                  titleSlug
                  title
                  difficulty
                }
              }
            }
            """;

    private static final String PROBLEM_DETAIL_QUERY = """
            query questionData($titleSlug: String!) {
              question(titleSlug: $titleSlug) {
                questionId
                title
                titleSlug
                content
                difficulty
                topicTags { name }
                exampleTestcases
                hints
              }
            }
            """;

    public List<QuestionSummary> fetchProblemList(String difficulty, int limit) {
        Map<String, Object> body = Map.of(
                "query", PROBLEM_LIST_QUERY,
                "variables", Map.of(
                        "categorySlug", "",
                        "limit", limit,
                        "skip", 0,
                        "filters", Map.of("difficulty", difficulty)
                )
        );

        try {
            ProblemListResponse response = leetCodeRestClient.post()
                    .uri("/graphql")
                    .body(body)
                    .retrieve()
                    .body(ProblemListResponse.class);

            failIfErrors(response == null ? null : response.errors(), "문제 목록");

            if (response == null || response.data() == null
                    || response.data().problemsetQuestionList() == null) {
                throw new ApiException(ErrorCode.PROBLEM_FETCH_FAILED);
            }

            return response.data().problemsetQuestionList().questions();
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("LeetCode 문제 목록 조회 실패: {}", e.getMessage());
            throw new ApiException(ErrorCode.PROBLEM_FETCH_FAILED);
        }
    }

    public QuestionDetail fetchProblemDetail(String titleSlug) {
        Map<String, Object> body = Map.of(
                "query", PROBLEM_DETAIL_QUERY,
                "variables", Map.of("titleSlug", titleSlug)
        );

        try {
            ProblemDetailResponse response = leetCodeRestClient.post()
                    .uri("/graphql")
                    .body(body)
                    .retrieve()
                    .body(ProblemDetailResponse.class);

            failIfErrors(response == null ? null : response.errors(), "문제 상세 [" + titleSlug + "]");

            if (response == null || response.data() == null) {
                throw new ApiException(ErrorCode.PROBLEM_FETCH_FAILED);
            }
            if (response.data().question() == null) {
                throw new ApiException(ErrorCode.PROBLEM_NOT_FOUND);
            }

            return response.data().question();
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("LeetCode 문제 상세 조회 실패 [{}]: {}", titleSlug, e.getMessage());
            throw new ApiException(ErrorCode.PROBLEM_FETCH_FAILED);
        }
    }

    private void failIfErrors(List<GraphQLError> errors, String context) {
        if (errors == null || errors.isEmpty()) {
            return;
        }
        String first = errors.get(0).message();
        log.warn("LeetCode GraphQL 오류 ({}): {}", context, first);
        throw new ApiException(ErrorCode.PROBLEM_FETCH_FAILED);
    }

    // Response records

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProblemListResponse(ProblemListData data, List<GraphQLError> errors) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProblemListData(
            @JsonProperty("problemsetQuestionList") ProblemListResult problemsetQuestionList
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProblemListResult(int total, List<QuestionSummary> questions) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record QuestionSummary(String title, String titleSlug, String difficulty) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProblemDetailResponse(ProblemDetailData data, List<GraphQLError> errors) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProblemDetailData(QuestionDetail question) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GraphQLError(String message) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record QuestionDetail(
            String questionId,
            String title,
            String titleSlug,
            String content,
            String difficulty,
            List<TopicTag> topicTags,
            String exampleTestcases,
            List<String> hints
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TopicTag(String name) {}
}
