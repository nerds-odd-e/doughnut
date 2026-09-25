package com.odde.donut.services.notebookTree;

/**
 * One named non-Markdown file with its folder placement and accepted Git content (standard Git LFS
 * pointer bytes, or empty for an empty file; never a hydrated LFS object).
 */
public record PortableTreeAttachmentRow(
    Integer folderId, String filename, byte[] acceptedGitContent) {}
