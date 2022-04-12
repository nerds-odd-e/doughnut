package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewSetting;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class ReviewSettingBuilder extends EntityBuilder<ReviewSetting> {
  private final Note note;

  public ReviewSettingBuilder(MakeMe makeMe, Note note) {
    super(makeMe, new ReviewSetting());
    this.note = note;
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    note.mergeMasterReviewSetting(entity);
    if (needPersist) {
      makeMe.modelFactoryService.entityManager.persist(note);
    }
  }

  public ReviewSettingBuilder level(int lvl) {
    this.entity.setLevel(lvl);
    return this;
  }
}
