package com.odde.donut.controllers.dto;

import com.odde.donut.services.ai.NoteRefinementLayout;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class NoteRefinementLayoutSelectionRequestDTO {
  public NoteRefinementLayout refinementLayout;
  public List<String> selectedItemIds;
}
