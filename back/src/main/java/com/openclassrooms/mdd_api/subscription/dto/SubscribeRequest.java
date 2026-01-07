package com.openclassrooms.mdd_api.subscription.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SubscribeRequest(
        @NotNull
        @Positive
        Long topicId
) {}
