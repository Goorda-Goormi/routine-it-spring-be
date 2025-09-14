package com.goormi.routine.admin.controller;

import com.goormi.routine.admin.dto.BulkDataResponse;
import com.goormi.routine.admin.service.BulkDataService;
import com.goormi.routine.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/bulk")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "벌크 데이터 생성", description = "성능 테스트를 위한 대량 데이터 생성 API")
public class BulkDataController {

	private final BulkDataService bulkDataService;

	@Operation(
		summary = "벌크 사용자 생성 (Master 담당)",
		description = "성능 테스트를 위해 지정된 수만큼 가상 사용자를 생성합니다. " +
			"생성되는 사용자들은 임의의 닉네임과 이메일을 가집니다."
	)
	@PostMapping("/users/{count}")
	public ApiResponse<BulkDataResponse> generateUsers(
		@Parameter(description = "생성할 사용자 수", required = true)
		@PathVariable int count) {

		log.info("Bulk users generation started - count: {}", count);

		BulkDataResponse response = bulkDataService.generateUsers(count);

		return ApiResponse.success("사용자 벌크 생성이 완료되었습니다.", response);
	}

	@Operation(
		summary = "벌크 그룹 생성",
		description = "성능 테스트를 위해 지정된 수만큼 그룹을 생성합니다. " +
			"각 그룹에는 랜덤한 멤버들이 할당됩니다."
	)
	@PostMapping("/groups/{count}")
	public ApiResponse<BulkDataResponse> generateGroups(
		@Parameter(description = "생성할 그룹 수", required = true)
		@PathVariable int count) {

		log.info("Bulk groups generation started - count: {}", count);

		BulkDataResponse response = bulkDataService.generateGroups(count);

		return ApiResponse.success("그룹 벌크 생성이 완료되었습니다.", response);
	}

	@Operation(
		summary = "벌크 채팅방 생성",
		description = "성능 테스트를 위해 지정된 수만큼 채팅방을 생성합니다."
	)
	@PostMapping("/chat-rooms/{count}")
	public ApiResponse<BulkDataResponse> generateChatRooms(
		@Parameter(description = "생성할 채팅방 수", required = true)
		@PathVariable int count) {

		log.info("Bulk chat rooms generation started - count: {}", count);

		BulkDataResponse response = bulkDataService.generateChatRooms(count);

		return ApiResponse.success("채팅방 벌크 생성이 완료되었습니다.", response);
	}

	@Operation(
		summary = "벌크 채팅 메시지 생성",
		description = "성능 테스트를 위해 지정된 수만큼 채팅 메시지를 생성합니다. " +
			"기존 채팅방들에 랜덤한 메시지가 생성됩니다."
	)
	@PostMapping("/chat-messages/{count}")
	public ApiResponse<BulkDataResponse> generateChatMessages(
		@Parameter(description = "생성할 메시지 수", required = true)
		@PathVariable int count) {

		log.info("Bulk chat messages generation started - count: {}", count);

		BulkDataResponse response = bulkDataService.generateChatMessages(count);

		return ApiResponse.success("채팅 메시지 벌크 생성이 완료되었습니다.", response);
	}

	@Operation(
		summary = "전체 벌크 데이터 생성",
		description = "성능 테스트를 위해 전체 시스템의 벌크 데이터를 생성합니다. " +
			"사용자, 루틴, 그룹, 채팅 메시지, 알림을 순서대로 생성합니다."
	)
	@PostMapping("/all")
	public ApiResponse<BulkDataResponse> generateAllBulkData(
		@Parameter(description = "생성할 사용자 수")
		@RequestParam(defaultValue = "1000") int userCount,
		@Parameter(description = "생성할 루틴 수")
		@RequestParam(defaultValue = "5000") int routineCount,
		@Parameter(description = "생성할 그룹 수")
		@RequestParam(defaultValue = "100") int groupCount,
		@Parameter(description = "생성할 채팅 메시지 수")
		@RequestParam(defaultValue = "10000") int messageCount,
		@Parameter(description = "생성할 알림 수")
		@RequestParam(defaultValue = "5000") int notificationCount) {

		log.info("Complete bulk data generation started");

		BulkDataResponse response = bulkDataService.generateAllBulkData(
			userCount, routineCount, groupCount, messageCount, notificationCount);

		return ApiResponse.success("전체 벌크 데이터 생성이 완료되었습니다.", response);
	}

	@Operation(
		summary = "벌크 데이터 삭제",
		description = "성능 테스트를 위해 생성된 벌크 데이터를 모두 삭제합니다. " +
			"주의: 실제 사용자 데이터는 삭제되지 않습니다."
	)
	@DeleteMapping("/cleanup")
	public ApiResponse<BulkDataResponse> cleanupBulkData() {
		log.info("Bulk data cleanup started");

		BulkDataResponse response = bulkDataService.cleanupBulkData();

		return ApiResponse.success("벌크 데이터 정리가 완료되었습니다.", response);
	}
}