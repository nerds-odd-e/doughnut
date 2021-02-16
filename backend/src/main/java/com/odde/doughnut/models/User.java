package com.odde.doughnut.models;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.Set;

@Entity
@Table(name = "user")
public class User {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
  @Getter @Setter private String name;
  @Column(name="external_identifier")
  @Getter @Setter private String externalIdentifier;

  @OneToMany(mappedBy="user")
  private Set<Note> notes;
}
