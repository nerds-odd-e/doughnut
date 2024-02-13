package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.factoryServices.ModelFactoryService;
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
import org.springframework.lang.Nullable;

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
  @Nullable
  private Circle circle;

  @OneToMany(mappedBy = "ownership")
  @SQLRestriction(value = "deleted_at is null")
  @JsonIgnore
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
      Timestamp currentUTCTimestamp,
      ModelFactoryService modelFactoryService,
      String topicConstructor) {
    final Note note =
        HierarchicalNote.createNote(user, null, currentUTCTimestamp, topicConstructor);
    note.buildNotebookForHeadNote(this, user);
    modelFactoryService.save(note.getNotebook());
    modelFactoryService.save(note);
    return note;
  }
}
