package com.odde.donut.services.notebookTree;

/** One named non-Markdown file with its folder placement and exact file bytes. */
public record PortableTreeAttachmentRow(Integer folderId, String filename, byte[] content) {}
