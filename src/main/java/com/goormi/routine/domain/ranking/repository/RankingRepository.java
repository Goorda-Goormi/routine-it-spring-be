package com.goormi.routine.domain.ranking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import com.goormi.routine.domain.ranking.entity.Ranking;

@Repository
public interface RankingRepository extends JpaRepository<Ranking, Long> {

	Optional<Ranking> findByUserId(Long userId);

	Optional<Ranking> findByGroupId(Long groupId);

	Optional<Ranking> findByUserIdAndGroupId(Long userId, Long groupId);

	@Query("SELECT r FROM Ranking r WHERE r.groupId IS NOT NULL ORDER BY r.score DESC")
	List<Ranking> findGroupRankingsOrderByScore();

	@Query("SELECT r FROM Ranking r WHERE r.groupId = :groupId AND r.userId IS NOT NULL ORDER BY r.score DESC LIMIT 3")
	List<Ranking> findTop3UsersByGroupId(@Param("groupId") Long groupId);

	@Query("SELECT r FROM Ranking r WHERE r.groupId = :groupId AND r.userId IS NOT NULL ORDER BY r.score DESC")
	List<Ranking> findAllUsersByGroupIdOrderByScore(@Param("groupId") Long groupId);

	@Query("SELECT r.groupId, SUM(r.score) as totalScore FROM Ranking r WHERE r.groupId IS NOT NULL GROUP BY r.groupId ORDER BY totalScore DESC")
	List<Object[]> findGroupTotalScoresOrderByScore();

	@Query("SELECT SUM(r.score) FROM Ranking r WHERE r.groupId = :groupId")
	Optional<Integer> getTotalScoreByGroupId(@Param("groupId") Long groupId);

	@Query("UPDATE Ranking r SET r.score = 0 WHERE r.groupId IS NULL")
	void resetPersonalScores();

	@Query("UPDATE Ranking r SET r.score = 0 WHERE r.groupId IS NOT NULL")
	void resetGroupScores();

	@Query("SELECT SUM(r.score) FROM Ranking r WHERE r.userId = :userId")
	Optional<Long> getTotalScoreByUserId(@Param("userId") Long userId);
}