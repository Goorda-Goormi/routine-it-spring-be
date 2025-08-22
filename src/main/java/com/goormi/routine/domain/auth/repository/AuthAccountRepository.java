package com.goormi.routine.domain.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.goormi.routine.domain.auth.entity.AuthAccount;

@Repository
public interface AuthAccountRepository extends JpaRepository<AuthAccount, Long> {
	Optional<AuthAccount> findByKakaoId(String kakaoId);
	Optional<AuthAccount> findByUser_UserId(Long userId);
	boolean existsByKakaoId(String kakaoId);
}