package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Entity
@Table(name = "ownership")
public class Ownership {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @OneToOne
  @JoinColumn(name = "user_id")
  @JsonIgnore
  @Setter
  private User user;

  @OneToOne
  @JoinColumn(name = "circle_id")
  @Setter
  private Circle circle;

  @OneToMany(mappedBy = "ownership")
  @SQLRestriction(value = "deleted_at is null")
  @JsonIgnore
  @Setter
  private List<Notebook> notebooks = new ArrayList<>();

  public String getOwnerName() {
    if (user != null) return user.getName();
    if (circle != null) return circle.getName();
    return null;
  }

  public boolean ownsBy(User user) {
    if (this.user != null) {
      return this.user.getId().equals(user.getId());
    }
    if (this.circle == null) return false;
    return this.circle.getMembers().contains(user);
  }

  public Notebook prepareNotebookForNewNotebook(
      User creator, Timestamp currentUTCTimestamp, String titleConstructor, String description) {
    Notebook notebook = new Notebook();
    notebook.setCreatorEntity(creator);
    notebook.setOwnership(this);
    notebook.setUpdated_at(currentUTCTimestamp);
    notebook.setCreatedAt(currentUTCTimestamp);
    if (titleConstructor != null) {
      String trimmed = titleConstructor.trim();
      if (!trimmed.isEmpty()) {
        notebook.setName(
            trimmed.length() > Note.MAX_TITLE_LENGTH
                ? trimmed.substring(0, Note.MAX_TITLE_LENGTH)
                : trimmed);
      }
    }
    if (description != null && !description.isBlank()) {
      notebook.setDescription(description.trim());
    }
    return notebook;
  }
}
