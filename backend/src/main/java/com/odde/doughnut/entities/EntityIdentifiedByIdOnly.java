package com.odde.doughnut.entities;

import jakarta.persistence.*;
import java.util.Objects;
import lombok.Getter;

@MappedSuperclass
public class EntityIdentifiedByIdOnly {
  @Id
  @Getter
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    EntityIdentifiedByIdOnly user = (EntityIdentifiedByIdOnly) o;
    return Objects.equals(id, user.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
