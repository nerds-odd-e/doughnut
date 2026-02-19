package com.odde.doughnut.entities;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "bazaar_notebook")
public class BazaarNotebook extends EntityIdentifiedByIdOnly {
  @ManyToOne(cascade = CascadeType.DETACH)
  @JoinColumn(name = "notebook_id", referencedColumnName = "id")
  @Getter
  @Setter
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull
  private Notebook notebook;
}
