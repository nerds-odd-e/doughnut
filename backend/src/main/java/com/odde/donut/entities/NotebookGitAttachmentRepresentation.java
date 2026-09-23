package com.odde.donut.entities;

/**
 * How a notebook's accepted Git binding stores non-Markdown attachment content: raw Git blobs, or
 * standard Git LFS pointers with content outside Git.
 */
public enum NotebookGitAttachmentRepresentation {
  RAW,
  LFS
}
