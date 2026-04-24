package org.project.cote.problem.dto;

import java.util.List;

public record ProblemDetailDto(
        String questionId,
        String title,
        String titleSlug,
        String content,
        String difficulty,
        List<String> tags,
        String exampleTestcases,
        List<String> hints,
        String leetcodeUrl
) {}
