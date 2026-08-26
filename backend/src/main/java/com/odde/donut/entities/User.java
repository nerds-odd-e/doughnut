package com.odde.donut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user")
public class User extends EntityIdentifiedByIdOnly {
  @NotNull @Getter @Setter private String name;

  @Column(name = "external_identifier")
  @Getter
  @Setter
  @NotNull
  private String externalIdentifier;

  @OneToMany(mappedBy = "user")
  @JsonIgnore
  @Getter
  @Setter
  private List<MemoryTracker> memoryTrackers = new ArrayList<>();

  @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
  @PrimaryKeyJoinColumn
  @Getter
  @Setter
  private Ownership ownership = new Ownership();

  @Column(name = "daily_assimilation_count")
  @Getter
  @Setter
  private Integer dailyAssimilationCount = 15;

  @Column(name = "health_remove_empty_folders_default")
  @Getter
  @Setter
  private Boolean healthRemoveEmptyFoldersDefault = false;

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
    return getSubscriptions().stream().anyMatch(s -> Objects.equals(s.getNotebook(), notebook));
  }

  public boolean inCircle(Circle circle) {
    return circle.getMembers().contains(this);
  }

  private static final List<String> allowUserIdentifiers =
      Arrays.asList("788834", "admin", "YeongSheng");

  public boolean isAdmin() {
    return allowUserIdentifiers.contains(getExternalIdentifier());
  }
}
