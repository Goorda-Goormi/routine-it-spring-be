package com.goormi.routine.domain.review.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goormi.routine.domain.review.dto.MonthlyReviewResponse;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiReviewServiceImpl implements AiReviewService{

	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;

	@Value("${gemini.api.key}")
	private String apiKey;

	@Value("${gemini.api.url}")
	private String geminiApiUrl;

	@Override
	public String generateAiMessage(MonthlyReviewResponse review) throws Exception {

		String reviewDataJson = objectMapper.writeValueAsString(review);
		String prompt = createGeminiPrompt(reviewDataJson);

		HttpHeaders headers = new org.springframework.http.HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		Map<String, Object> content = Map.of(
			"role", "user",
			"parts", List.of(Map.of("text", prompt))
		);

		Map<String, Object> body = Map.of(
			"contents", List.of(content)
		);

		HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

		String urlWithKey = geminiApiUrl + "?key=" + apiKey;

		try {
			Map response = restTemplate.postForObject(
				urlWithKey,
				request,
				Map.class
			);

			return extractMessageFromGeminiResponse(response);
		} catch (Exception e) {
			throw e;
		}
	}

	private String createGeminiPrompt(String reviewDataJson) {
		return "당신은 사용자 맞춤형 루틴 코치입니다. 다음 JSON 데이터를 기반으로 월간 회고 메시지를 한국어로 작성해 주세요. " +
			"메시지는 최대 500자로 제한하며, 매우 친근하고 격려하는 톤으로 작성해야 합니다. " +
			"메시지의 구조는 다음 세 가지 단계를 **반드시** 따라야 합니다:\n\n" +

			"1. **[주요 성과 요약 (데이터 명료화)]**: " +
			"   - '총 점수', '참여 그룹 수', '개인 루틴 달성률', '총 인증 횟수' 등의 핵심 수치를 '📈 이번 달 성과' 같은 명료한 제목 하에 구조화된 목록 형태로 먼저 제시해야 합니다. " +
			"   - 'scoreDifference'와 'groupDifference' 수치(지난 달 대비 변화)를 괄호 안에 강조하여 표시해야 합니다.\n" +

			"2. **[데이터 해석 및 격려]**: " +
			"   - 제시한 핵심 수치들을 기반으로 사용자의 노력과 성과를 칭찬하고 격려하는 문장을 2~3줄로 자연스럽게 연결해 작성합니다. " +
			"   - 특히 'personalRoutineAchievementRate'가 90% 이상이면 '완벽한 실천력'으로, 'scoreDifference'가 양수이면 '눈부신 발전'으로 강조합니다.\n" +

			"3. **[다음 달 목표 제시 및 마무리]**: " +
			"   - 사용자의 현재 성과(예: 낮은 달성률, 그룹 미참여, 높은 점수)를 바탕으로 다음 달에 도전할 수 있는 구체적이고 긍정적인 목표(Goal Setting)를 제안합니다. " +
			"   - 마지막은 다음 달도 함께 성장하자는 긍정적인 문장으로 마무리해야 합니다. " +

			"제공된 데이터: " + reviewDataJson;
	}

	private String extractMessageFromGeminiResponse(Map response) {
		try {
			List candidates = (List)response.get("candidates");
			if (candidates == null || candidates.isEmpty()) {
				Map promptFeedback = (Map)response.get("promptFeedback");
				String blockReason = (String)promptFeedback.get("blockReason");
				throw new IllegalArgumentException("Gemini 응답에 텍스트가 없습니다. 차단 사유: " + blockReason);
			}
			Map candidate = (Map)candidates.get(0);
			Map content = (Map)candidate.get("content");
			List parts = (List)content.get("parts");
			Map part = (Map)parts.get(0);

			return (String)part.get("text");
		} catch (Exception e) {
			throw new RuntimeException("AI 응답을 처리할 수 없습니다.", e);
		}
	}

}
