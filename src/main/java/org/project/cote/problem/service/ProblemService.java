package org.project.cote.problem.service;

import lombok.RequiredArgsConstructor;
import org.project.cote.common.dto.ErrorCode;
import org.project.cote.common.exception.ApiException;
import org.project.cote.problem.client.LeetCodeClient;
import org.project.cote.problem.client.LeetCodeClient.QuestionDetail;
import org.project.cote.problem.client.LeetCodeClient.QuestionSummary;
import org.project.cote.problem.dto.Difficulty;
import org.project.cote.problem.dto.ProblemDetailDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class ProblemService {

    private static final int FETCH_LIMIT = 100;
    private static final String LEETCODE_BASE_URL = "https://leetcode.com/problems/";

    private final LeetCodeClient leetCodeClient;

    public ProblemDetailDto getRandomProblem(Difficulty difficulty) {
        List<QuestionSummary> problems = leetCodeClient.fetchProblemList(difficulty.name(), FETCH_LIMIT);

        if (problems == null || problems.isEmpty()) {
            throw new ApiException(ErrorCode.NO_PROBLEMS_AVAILABLE);
        }

        int randomIndex = ThreadLocalRandom.current().nextInt(problems.size());
        String titleSlug = problems.get(randomIndex).titleSlug();

        QuestionDetail detail = leetCodeClient.fetchProblemDetail(titleSlug);

        return toDto(detail);
    }

    private ProblemDetailDto toDto(QuestionDetail detail) {
        List<String> tags = detail.topicTags() == null ? List.of()
                : detail.topicTags().stream().map(LeetCodeClient.TopicTag::name).toList();

        return new ProblemDetailDto(
                detail.questionId(),
                detail.title(),
                detail.titleSlug(),
                detail.content(),
                detail.difficulty(),
                tags,
                detail.exampleTestcases(),
                detail.hints() == null ? List.of() : detail.hints(),
                LEETCODE_BASE_URL + detail.titleSlug()
        );
    }
}
