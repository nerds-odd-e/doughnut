package com.odde.donut.mcp;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import com.odde.donut.services.PortablePathAuthoring;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
class McpNoteCandidates {
  private final PortablePathAuthoring portablePathAuthoring;

  McpNoteCandidates(PortablePathAuthoring portablePathAuthoring) {
    this.portablePathAuthoring = portablePathAuthoring;
  }

  Map<String, Object> describe(Note note, User viewer) {
    Map<String, Object> candidate = new LinkedHashMap<>();
    candidate.put("portablePath", portablePathAuthoring.qualifiedPortablePath(note, viewer));
    candidate.put("title", note.getTitle());
    candidate.put("notebookName", note.getNotebook().getName());
    candidate.put("folderPath", String.join("/", note.folderNamesFromRoot()));
    return candidate;
  }
}
