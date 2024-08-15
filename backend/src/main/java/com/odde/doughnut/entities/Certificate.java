package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "certificate")
@Getter
@Setter
public class Certificate extends EntityIdentifiedByIdOnly {
  @ManyToOne
  @JoinColumn(name = "notebook_id")
  private Notebook notebook;

  @ManyToOne
  @JoinColumn(name = "user_id")
  @JsonIgnore
  private User user;

  @Column(name = "expiry_date")
  private Timestamp expiryDate;
}
