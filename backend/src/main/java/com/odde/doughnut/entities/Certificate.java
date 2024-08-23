package com.odde.doughnut.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "certificate")
@Getter
@Setter
public class Certificate extends EntityIdentifiedByIdOnly {

  @ManyToOne
  @JoinColumn(name = "user_id")
  private User user;

  @ManyToOne
  @JoinColumn(name = "notebook_id")
  private Notebook notebook;

  @Column(name = "start_date")
  @NotNull
  private Timestamp startDate;

  @Column(name = "expiry_date")
  @NotNull
  private Timestamp expiryDate;

  public String getCreatorName() {
    return notebook.getCreatorEntity().getName();
  }
}
