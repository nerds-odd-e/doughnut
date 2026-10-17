package com.odde.donut.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.sql.Timestamp;

public record AttributedNoteContent(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Integer noteId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String notebookName,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String folderPath,
    String content,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Timestamp updatedAt,
    @Schema(
            description =
                "The notebook's accepted Git commit id when the note was read; null when the notebook has no Git binding.")
        String acceptedRevision) {}
