package com.goormi.routine.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkDataResponse {

	private boolean success;
	private String message;
	private int generatedCount;
	private int deletedCount;
	private long executionTimeMs;
	private LocalDateTime executedAt;
	private String dataType;

	private Map<String, Object> details;

	public static BulkDataResponse success(String dataType, int count, long executionTime) {
		return BulkDataResponse.builder()
			.success(true)
			.message(dataType + " 벌크 데이터 생성 완료")
			.dataType(dataType)
			.generatedCount(count)
			.executionTimeMs(executionTime)
			.executedAt(LocalDateTime.now())
			.build();
	}

	public static BulkDataResponse cleanup(int deletedCount, long executionTime) {
		return BulkDataResponse.builder()
			.success(true)
			.message("벌크 데이터 정리 완료")
			.dataType("cleanup")
			.deletedCount(deletedCount)
			.executionTimeMs(executionTime)
			.executedAt(LocalDateTime.now())
			.build();
	}

	public static BulkDataResponse error(String dataType, String errorMessage) {
		return BulkDataResponse.builder()
			.success(false)
			.message("벌크 데이터 생성 실패: " + errorMessage)
			.dataType(dataType)
			.executedAt(LocalDateTime.now())
			.build();
	}
}