package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.Players;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class PlayersBuilder extends EntityBuilder<Players> {

  public PlayersBuilder(MakeMe makeMe) {
    super(makeMe, new Players());
  }

  public PlayersBuilder(Players players, MakeMe makeMe) {
    super(makeMe, players);
  }

  public PlayersBuilder name(String name) {
    entity.setName(name);
    return this;
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    if (entity.getName() == null) {
      entity.setName("Player" + System.currentTimeMillis());
    }
  }
}
