package com.odde.donut.services.notebookExport;

/**
 * One file in the canonical Portable-tree snapshot of a notebook export: its final path (relative
 * to the notebook root, folders joined with "/") and its exact final file content.
 */
public record PortableTreeEntry(String path, String content) {}
