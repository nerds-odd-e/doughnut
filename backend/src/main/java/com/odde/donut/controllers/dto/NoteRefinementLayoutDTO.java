package com.odde.donut.controllers.dto;

import com.odde.donut.services.ai.NoteRefinementLayout;
import com.odde.donut.services.ai.NoteRefinementLayoutItem;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class NoteRefinementLayoutDTO {
  private List<NoteRefinementLayoutItem> items;

  public NoteRefinementLayoutDTO(NoteRefinementLayout layout) {
    this.items = layout.getItems();
  }
}
