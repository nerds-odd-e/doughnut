package com.odde.doughnut.entities;

import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.util.Objects;
import javax.persistence.*;
import lombok.Getter;

@MappedSuperclass
public abstract class EntityIdentifiedByIdOnly {
  @Id
  @Getter
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  protected Integer id;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    EntityIdentifiedByIdOnly entity = (EntityIdentifiedByIdOnly) o;
    return Objects.equals(id, entity.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  public void beforeCommit(ModelFactoryService modelFactoryService) {}

  public void beforeCreate(ModelFactoryService modelFactoryService) {}

  public void beforeUpdate(ModelFactoryService modelFactoryService) {}
}
