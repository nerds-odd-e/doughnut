package com.odde.donut.controllers.dto;

import jakarta.validation.constraints.NotNull;

/** One file as it appears in a listing: its id and complete filename, never its content. */
public record NotebookAttachmentListItem(@NotNull Integer id, @NotNull String filename) {}
