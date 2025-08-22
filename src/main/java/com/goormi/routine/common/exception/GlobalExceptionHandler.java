package com.goormi.routine.common.exception;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.goormi.routine.common.response.ErrorResponse;
import com.goormi.routine.domain.user.exception.DuplicateNicknameException;
import com.goormi.routine.domain.user.exception.UserNotFoundException;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	@ExceptionHandler(UserNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleUserNotFoundException(UserNotFoundException e) {
		log.warn("User not found: {}", e.getMessage());
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
			.body(new ErrorResponse("사용자를 찾을 수 없습니다"));
	}

	@ExceptionHandler(DuplicateNicknameException.class)
	public ResponseEntity<ErrorResponse> handleDuplicateNicknameException(DuplicateNicknameException e) {
		log.warn("Duplicate nickname: {}", e.getMessage());
		return ResponseEntity.status(HttpStatus.CONFLICT)
			.body(new ErrorResponse(e.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
		String message = e.getBindingResult().getFieldErrors().stream()
			.map(FieldError::getDefaultMessage)
			.collect(Collectors.joining(", "));
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(new ErrorResponse(message));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGeneralException(Exception e) {
		log.error("Unexpected error occurred", e);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(new ErrorResponse("서버 내부 오류가 발생했습니다"));
	}
}
