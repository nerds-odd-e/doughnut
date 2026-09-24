package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.Folder;

/** A file's id and its folder (null at the notebook root), without its content. */
public record NotebookAttachmentPlacement(Integer id, Folder folder) {}
