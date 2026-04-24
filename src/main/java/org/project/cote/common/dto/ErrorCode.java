package org.project.cote.common.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),

    // Problem
    PROBLEM_NOT_FOUND(HttpStatus.NOT_FOUND, "문제를 찾을 수 없습니다."),
    PROBLEM_FETCH_FAILED(HttpStatus.BAD_GATEWAY, "LeetCode에서 문제를 가져오는 데 실패했습니다."),
    NO_PROBLEMS_AVAILABLE(HttpStatus.NOT_FOUND, "해당 조건에 맞는 문제가 없습니다.");

    private final HttpStatus status;
    private final String message;
}
