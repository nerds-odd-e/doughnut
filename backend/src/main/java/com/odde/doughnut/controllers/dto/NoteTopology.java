package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.entities.RelationType;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.NonNull;

@NoArgsConstructor
@Data
public class NoteTopology {
  @NonNull private Integer id;
  @NonNull private String title;
  private String shortDetails;
  private RelationType relationType;
  private NoteTopology targetNoteTopology;
  private NoteTopology parentOrSubjectNoteTopology;

  public int getId() {
    return this.id;
  }
}
