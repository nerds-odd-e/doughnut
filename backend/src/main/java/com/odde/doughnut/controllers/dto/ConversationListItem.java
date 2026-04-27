package com.odde.doughnut.controllers.dto;

import jakarta.validation.constraints.NotNull;

public record ConversationListItem(
    @NotNull Integer id, @NotNull String subject, String partnerName) {}
