package com.goormi.routine.admin.service;

import com.goormi.routine.admin.dto.BulkDataResponse;

public interface BulkDataService {


	BulkDataResponse generateUsers(int count);

	/**
	 * 벌크 개인 루틴 생성
	 */
	BulkDataResponse generatePersonalRoutines(int count);

	/**
	 * 벌크 그룹 생성
	 */
	BulkDataResponse generateGroups(int count);

	/**
	 * 벌크 채팅 메시지 생성
	 */
	BulkDataResponse generateChatMessages(int count);

	/**
	 * 벌크 채팅방 생성
	 */
	BulkDataResponse generateChatRooms(int count);

	/**
	 * 벌크 알림 생성
	 */
	BulkDataResponse generateNotifications(int count);

	/**
	 * 전체 벌크 데이터 생성
	 */
	BulkDataResponse generateAllBulkData(int userCount, int routineCount,
		int groupCount, int messageCount,
		int notificationCount);

	/**
	 * 벌크 데이터 정리
	 */
	BulkDataResponse cleanupBulkData();
}