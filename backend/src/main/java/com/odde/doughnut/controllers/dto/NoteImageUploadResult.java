package com.odde.doughnut.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "NoteImageUploadResult")
public record NoteImageUploadResult(
    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            description =
                "Path suitable for the note frontmatter `image:` scalar (e.g. /attachments/images/{id}/{filename}).")
        String imagePath) {}
