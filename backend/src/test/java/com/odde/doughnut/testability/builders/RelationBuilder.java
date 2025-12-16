package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.services.NoteService;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;

public class RelationBuilder extends EntityBuilder<Note> {
  public RelationBuilder(MakeMe makeMe) {
    super(makeMe, null);
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    throw new UnsupportedOperationException("Please use between method");
  }

  public NoteBuilder between(Note from, Note to, RelationType relationType) {
    this.entity =
        NoteService.buildALink(
            from, to, null, relationType, new Timestamp(System.currentTimeMillis()));
    return new NoteBuilder(entity, makeMe);
  }

  public NoteBuilder between(Note from, Note to) {
    return between(from, to, RelationType.SPECIALIZE);
  }
}
