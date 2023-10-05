package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
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
      return this.user.equals(user);
    }
    if (this.circle == null) return false;
    return this.circle.getMembers().contains(user);
  }

  public Note createNotebook(User user, TextContent textContent, Timestamp currentUTCTimestamp) {
    final Note note = Note.createNote(user, currentUTCTimestamp, textContent);
    note.buildNotebookForHeadNote(this, user);
    return note;
  }
}
