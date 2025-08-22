package com.goormi.routine.domain.user.exception;

public class DuplicateNicknameException extends RuntimeException {
	public DuplicateNicknameException(String nickname) {
		super("이미 사용중인 닉네임입니다: " + nickname);
	}
}