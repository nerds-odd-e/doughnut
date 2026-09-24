package com.odde.donut.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "NoteImageUploadResult")
public record NoteImageUploadResult(
    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            description =
                "The filename the upload wrote into the note's frontmatter `image:`; the picture is a file of that name in the note's folder.")
        String imagePath) {}
