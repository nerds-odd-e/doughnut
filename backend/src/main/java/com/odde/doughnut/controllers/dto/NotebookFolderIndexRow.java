package com.odde.doughnut.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    name = "NotebookFolderIndexRow",
    description = "Flat folder row for building folder trees and paths within a notebook.")
public record NotebookFolderIndexRow(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Integer id,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
    @Schema(description = "Null when the folder is at notebook root.") Integer parentFolderId) {}
