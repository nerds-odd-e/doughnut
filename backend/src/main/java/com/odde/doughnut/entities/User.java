package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.algorithms.SpacedRepetitionAlgorithm;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user")
public class User {

  @Id
  @Getter
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @NotNull
  @Size(min = 1, max = 100)
  @Getter
  @Setter
  private String name;

  @Column(name = "external_identifier")
  @Getter
  @Setter
  private String externalIdentifier;

  @OneToMany(mappedBy = "user")
  @JsonIgnore
  @Getter
  @Setter
  private List<ReviewPoint> reviewPoints = new ArrayList<>();

  @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
  @PrimaryKeyJoinColumn
  @Getter
  @Setter
  private Ownership ownership = new Ownership();

  @Column(name = "daily_new_notes_count")
  @Getter
  @Setter
  private Integer dailyNewNotesCount = 15;

  @Pattern(regexp = "^\\d+(,\\s*\\d+)*$", message = "must be numbers separated by ','")
  @Column(name = "space_intervals")
  @Getter
  @Setter
  private String spaceIntervals = "0, 1, 1, 2, 3, 5, 8, 13, 21, 34, 55";

  @JoinTable(
      name = "circle_user",
      joinColumns = {@JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)},
      inverseJoinColumns = {
        @JoinColumn(name = "circle_id", referencedColumnName = "id", nullable = false)
      })
  @ManyToMany
  @JsonIgnore
  @Getter
  private final List<Circle> circles = new ArrayList<>();

  @OneToMany(mappedBy = "user", cascade = CascadeType.DETACH)
  @JsonIgnore
  @Getter
  private final List<Subscription> subscriptions = new ArrayList<>();

  public User() {
    ownership.setUser(this);
  }

  public boolean owns(Notebook notebook) {
    return notebook.getOwnership().ownsBy(this);
  }

  public boolean canReferTo(Notebook notebook) {
    if (owns(notebook)) return true;
    return getSubscriptions().stream().anyMatch(s -> s.getNotebook() == notebook);
  }

  public boolean inCircle(Circle circle) {
    return circle.getMembers().contains(this);
  }

  @JsonIgnore
  public SpacedRepetitionAlgorithm getSpacedRepetitionAlgorithm() {
    return new SpacedRepetitionAlgorithm(getSpaceIntervals());
  }
}
