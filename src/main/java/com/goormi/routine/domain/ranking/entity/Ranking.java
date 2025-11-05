package com.goormi.routine.domain.ranking.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.UpdateTimestamp;

import com.goormi.routine.domain.group.entity.Group;
import com.goormi.routine.domain.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "rankings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ranking {
	@Id
	@Column(name = "ranking_id")
	private Long rankingId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "group_id")
	private Long groupId;

	@Column(name = "score", nullable = false)
	@Builder.Default
	private Integer score = 0;

	@Column(name = "consecutive_days")
	@Builder.Default
	private Integer consecutiveDays = 0;

	@Column(name = "last_auth_date")
	private LocalDate lastAuthDate;

	@Column(name = "month_year", nullable = false, length = 7)
	private String monthYear;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", insertable = false, updatable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "group_id", insertable = false, updatable = false)
	private Group group;
}