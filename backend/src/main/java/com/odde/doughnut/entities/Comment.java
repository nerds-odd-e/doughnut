package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@AllArgsConstructor
@Table(name = "comment")
public class Comment {
  @Id
  @Getter
  @JsonIgnore
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Getter
  @Setter
  @Column(name = "text")
  String text;

  //  @ManyToOne(cascade = CascadeType.PERSIST)
  //  @JoinColumn(name = "note_id")
  @Column(name = "note_id")
  @Getter
  @Setter
  Integer note_id;

  @Column(name = "created_at")
  @Getter
  @Setter
  @JsonIgnore
  private Timestamp createdAt = new Timestamp(System.currentTimeMillis());



}
