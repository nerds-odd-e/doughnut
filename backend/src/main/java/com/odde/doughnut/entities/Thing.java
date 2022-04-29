package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.sql.Timestamp;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "thing")
public class Thing {
  @Id
  @Getter
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "created_at")
  @Setter
  private Timestamp createdAt;

  @Column(name = "deleted_at")
  @Setter
  private Timestamp deletedAt;

  @OneToOne(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "note_id", referencedColumnName = "id")
  @Getter
  @Setter
  private Note note;

  @OneToOne
  @JoinColumn(name = "user_id")
  @JsonIgnore
  @Getter
  @Setter
  private User user;
}
