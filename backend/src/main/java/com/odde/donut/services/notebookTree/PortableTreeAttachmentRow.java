package com.odde.donut.services.notebookTree;

/**
 * One named non-Markdown file kept at the notebook root: its complete filename, extension included,
 * and its exact file bytes. Nothing decodes or classifies the bytes.
 */
public record PortableTreeAttachmentRow(String filename, byte[] content) {}
