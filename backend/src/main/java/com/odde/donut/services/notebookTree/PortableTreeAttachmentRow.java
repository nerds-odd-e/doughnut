package com.odde.donut.services.notebookTree;

/**
 * One named non-Markdown file with its folder placement and accepted Git content (legacy raw
 * payload bytes or standard Git LFS pointer bytes; never a hydrated LFS object).
 */
public record PortableTreeAttachmentRow(
    Integer folderId, String filename, byte[] acceptedGitContent) {}
