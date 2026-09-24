package com.odde.donut.controllers.dto;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.NotebookAttachment;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(
    description =
        "Note topologies, child folders, and files in a structural listing scope (e.g. notebook root or"
            + " a folder).")
public record FolderListing(
    List<NoteTopology> noteTopologies,
    List<Folder> folders,
    List<NotebookAttachment> attachments) {}
