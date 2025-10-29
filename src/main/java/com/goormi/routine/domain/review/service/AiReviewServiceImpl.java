package com.goormi.routine.domain.review.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.BlockedReason;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.GenerateContentResponsePromptFeedback;
import com.google.genai.types.Part;
import com.goormi.routine.domain.review.dto.MonthlyReviewResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiReviewServiceImpl implements AiReviewService{

	private final Client geminiClient;
	private final ObjectMapper objectMapper;

	// @Value("${gemini.api.key}")
	// private String apiKey;
	//
	// @Value("${gemini.api.url}")
	// private String geminiApiUrl;

	private static final String MODEL_NAME = "gemini-2.5-flash";

	@Override
	public String generateAiMessage(MonthlyReviewResponse review) throws Exception {

		String reviewDataJson = objectMapper.writeValueAsString(review);
		String prompt = createGeminiPrompt(reviewDataJson);

		Content userContent = Content.builder()
			.role("user")
			.parts(List.of(Part.builder().text(prompt).build()))
			.build();

		try {
			GenerateContentResponse response = geminiClient.models.generateContent(
				MODEL_NAME,
				List.of(userContent),
				null
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

	private String extractMessageFromGeminiResponse(GenerateContentResponse response) {
			String generatedText = response.text();

			if (generatedText != null && !generatedText.trim().isEmpty()) {
				return generatedText;
			}

			Optional<GenerateContentResponsePromptFeedback> feedbackOptional = response.promptFeedback();

			String blockReason = "알 수 없음 (텍스트 생성 실패)";

			if (feedbackOptional.isPresent()) {
				GenerateContentResponsePromptFeedback feedback = feedbackOptional.get();

				Optional<BlockedReason> reasonOptional = feedback.blockReason();

				if (reasonOptional.isPresent()) {
					blockReason = "차단됨: " + reasonOptional.get().toString();
				} else {
					int count = feedback.safetyRatings()
						.map(List::size)
						.orElse(0);

					blockReason = "안전 필터링 의심 (등급 " + count + "개 확인)";
				}
			}
				log.error("Gemini 텍스트 생성 실패. Fallback으로 전환됩니다. 사유: {}", blockReason);

				throw new IllegalStateException("AI 메시지 생성 실패: " + blockReason);
			}
	}