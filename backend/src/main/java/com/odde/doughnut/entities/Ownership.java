package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.controllers.dto.NotebooksViewedByUser;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
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

  public Note createAndPersistNotebook(
      User user,
      Timestamp currentUTCTimestamp,
      NoteRepository noteRepository,
      EntityPersister entityPersister,
      String titleConstructor) {
    final Note note = new Note();
    note.initialize(user, null, currentUTCTimestamp, titleConstructor);
    note.buildNotebookForHeadNote(this, user);
    entityPersister.save(note.getNotebook());
    entityPersister.save(note);
    return note;
  }

  public NotebooksViewedByUser jsonNotebooksViewedByUser(List<Notebook> allNotebooks) {
    NotebooksViewedByUser notebooksViewedByUser = new NotebooksViewedByUser();
    notebooksViewedByUser.notebooks = allNotebooks;
    return notebooksViewedByUser;
  }
}
