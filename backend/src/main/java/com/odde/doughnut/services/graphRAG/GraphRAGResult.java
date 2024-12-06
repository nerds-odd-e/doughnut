package com.odde.doughnut.services.graphRAG;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GraphRAGResult {
  private FocusNote focusNote;
  private List<BareNote> relatedNotes = new ArrayList<>();
}
