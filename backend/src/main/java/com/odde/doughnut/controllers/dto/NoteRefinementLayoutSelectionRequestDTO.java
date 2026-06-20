package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.services.ai.NoteRefinementLayout;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class NoteRefinementLayoutSelectionRequestDTO {
  public NoteRefinementLayout layout;
  public List<String> selectedItemIds;
}
