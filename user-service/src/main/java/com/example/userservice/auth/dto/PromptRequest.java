package com.example.userservice.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PromptRequest {

    @NotBlank
    private String prompt;

    /**
     * Optional external conversation identifier so prompts can be saved to a specific chat.
     */
    private String chatId;

    /**
     * Optional limit for data queries.
     */
    private Integer limit;
}


