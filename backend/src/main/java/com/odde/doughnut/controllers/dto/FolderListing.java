package com.odde.doughnut.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(
    description =
        "Note topologies and child folders in a structural listing scope (e.g. notebook root or"
            + " a folder).")
public record FolderListing(List<NoteTopology> noteTopologies, List<NotebookRootFolder> folders) {}
