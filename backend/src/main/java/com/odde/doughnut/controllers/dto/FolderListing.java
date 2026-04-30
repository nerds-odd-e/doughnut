package com.odde.doughnut.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(
    description =
        "Notes and child folders in a structural listing scope (e.g. notebook root or a folder).")
public record FolderListing(List<NoteRealm> notes, List<NotebookRootFolder> folders) {}
