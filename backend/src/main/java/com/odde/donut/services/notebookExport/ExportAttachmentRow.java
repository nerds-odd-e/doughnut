package com.odde.donut.services.notebookExport;

/**
 * One named non-Markdown file kept at the notebook root: its complete filename, extension included,
 * and its exact file bytes. Nothing decodes or classifies the bytes.
 */
public record ExportAttachmentRow(String filename, byte[] content) {}
