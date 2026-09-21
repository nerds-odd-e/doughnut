package com.odde.donut.services.notebookExport;

/** One named non-Markdown file with its folder placement and exact file bytes. */
public record ExportAttachmentRow(Integer folderId, String filename, byte[] content) {}
