package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;

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
  private Circle circle;

  @OneToMany(mappedBy = "ownership")
  @Where(clause = "deleted_at is null")
  @JsonIgnore
  @Getter
  @Setter
  private List<Notebook> notebooks = new ArrayList<>();

  public Optional<Circle> getCircle() {
    return Optional.ofNullable(circle);
  }

  public boolean ownsBy(User user) {
    if (this.user != null) {
      return this.user.equals(user);
    }
    return getCircle().map((circle) -> circle.getMembers().contains(user)).orElse(false);
  }

  public Note createNotebook(User user, TextContent textContent, Timestamp currentUTCTimestamp) {
    final Note note = Note.createNote(user, currentUTCTimestamp, textContent);
    note.buildNotebookForHeadNote(this, user);
    return note;
  }
}
