package com.odde.donut.mcp;

import com.odde.donut.controllers.dto.AttributedNoteContent;
import java.sql.Timestamp;

record McpAttributedNoteContent(
    String portablePath,
    String title,
    String notebookName,
    String folderPath,
    String content,
    Timestamp updatedAt,
    String acceptedRevision) {
  static McpAttributedNoteContent from(AttributedNoteContent content, String portablePath) {
    return new McpAttributedNoteContent(
        portablePath,
        content.title(),
        content.notebookName(),
        content.folderPath(),
        content.content(),
        content.updatedAt(),
        content.acceptedRevision());
  }
}
