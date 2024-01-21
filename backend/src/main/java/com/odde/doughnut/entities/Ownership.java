package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.odde.doughnut.factoryServices.ModelFactoryService;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;
import org.springframework.lang.Nullable;

@Entity
@Table(name = "ownership")
public class Ownership {
  @Id
  @Getter
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @OneToOne
  @JoinColumn(name = "user_id")
  @JsonIgnore
  @Getter
  @Setter
  private User user;

  @OneToOne
  @JoinColumn(name = "circle_id")
  @Setter
  @Getter
  @Nullable
  private Circle circle;

  @OneToMany(mappedBy = "ownership")
  @Where(clause = "deleted_at is null")
  @JsonIgnore
  @Getter
  @Setter
  private List<Notebook> notebooks = new ArrayList<>();

  public boolean ownsBy(User user) {
    if (this.user != null) {
      return this.user.getId().equals(user.getId());
    }
    if (this.circle == null) return false;
    return this.circle.getMembers().contains(user);
  }

  public Note createAndPersistNotebook(
      User user,
      TextContent textContent,
      Timestamp currentUTCTimestamp,
      ModelFactoryService modelFactoryService) {
    final Note note = Note.createNote(user, currentUTCTimestamp, textContent);
    note.buildNotebookForHeadNote(this, user);
    modelFactoryService.createRecord(note.getNotebook());
    modelFactoryService.toNoteModel(note).save();
    return note;
  }
}
