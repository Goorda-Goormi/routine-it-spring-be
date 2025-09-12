package com.goormi.routine.domain.ranking.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import com.goormi.routine.domain.ranking.entity.Ranking;

@Repository
public interface RankingRepository extends JpaRepository<Ranking, Long> {

	Optional<Ranking> findByUserIdAndGroupId(Long userId, Long groupId);

	@Query("SELECT r FROM Ranking r WHERE r.groupId = :groupId AND r.userId IS NOT NULL ORDER BY r.score DESC LIMIT 3")
	List<Ranking> findTop3UsersByGroupId(@Param("groupId") Long groupId);

	@Query("SELECT r FROM Ranking r WHERE r.groupId = :groupId AND r.userId IS NOT NULL ORDER BY r.score DESC")
	List<Ranking> findAllUsersByGroupIdOrderByScore(@Param("groupId") Long groupId);

	@Query(value = "SELECT r.user_id, r.score FROM rankings r WHERE r.month_year = :monthYear AND r.group_id IS NOT NULL ORDER BY r.score DESC",
		countQuery = "SELECT COUNT(*) FROM rankings r WHERE r.month_year = :monthYear AND r.group_id IS NOT NULL",
		nativeQuery = true)
	Page<Object[]> findPersonalRankingsByMonth(@Param("monthYear") String monthYear, Pageable pageable);

	@Query("SELECT r.groupId, g.groupName, g.category, g.groupType, SUM(r.score) as totalScore " +
		"FROM Ranking r JOIN r.group g " +
		"WHERE r.monthYear = :monthYear " +
		"AND r.groupId IS NOT NULL " +
		"AND (:category IS NULL OR g.category = :category) " +
		"AND (:groupType IS NULL OR g.groupType = :groupType) " +
		"GROUP BY r.groupId, g.groupName, g.category, g.groupType " +
		"ORDER BY totalScore DESC")
	Page<Object[]> findGroupRankingsByMonthAndFilters(@Param("monthYear") String monthYear,
		@Param("category") String category,
		@Param("groupType") String groupType,
		Pageable pageable);

	@Query("SELECT r FROM Ranking r WHERE r.groupId = :groupId AND r.monthYear = :monthYear ORDER BY r.score DESC")
	List<Ranking> findAllUsersByGroupIdAndMonthOrderByScore(@Param("groupId") Long groupId, @Param("monthYear") String monthYear);

}