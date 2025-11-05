package com.goormi.routine.domain.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddReactionRequest {

    @NotNull(message = "메시지 ID는 필수입니다")
    private Long messageId;

    @NotBlank(message = "이모지는 필수입니다")
    @Size(max = 10, message = "이모지는 최대 10자까지 가능합니다")
    private String emoji;
}