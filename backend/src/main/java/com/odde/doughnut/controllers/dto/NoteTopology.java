package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.entities.LinkType;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.NonNull;

@NoArgsConstructor
@Data
public class NoteTopology {
  @NonNull private Integer id;
  @NonNull private String titleOrPredicate;
  private String shortDetails;
  private LinkType linkType;
  private NoteTopology targetNoteTopology;
  private NoteTopology parentOrSubjectNoteTopology;

  public int getId() {
    return this.id;
  }
}
