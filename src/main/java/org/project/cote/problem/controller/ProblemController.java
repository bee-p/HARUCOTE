package org.project.cote.problem.controller;

import lombok.RequiredArgsConstructor;
import org.project.cote.common.dto.ApiResponse;
import org.project.cote.problem.dto.Difficulty;
import org.project.cote.problem.dto.ProblemDetailDto;
import org.project.cote.problem.service.ProblemService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/problems")
@RequiredArgsConstructor
public class ProblemController {

    private final ProblemService problemService;

    @GetMapping("/random")
    public ResponseEntity<ApiResponse<ProblemDetailDto>> getRandomProblem(
            @RequestParam(defaultValue = "EASY") Difficulty difficulty
    ) {
        ProblemDetailDto problem = problemService.getRandomProblem(difficulty);
        return ResponseEntity.ok(ApiResponse.ok(problem));
    }
}
