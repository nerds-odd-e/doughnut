package com.odde.doughnut.entities;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "subscription")
public class SubscriptionEntity {
    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;

    @Column(name = "daily_target_of_new_notes")
    @Getter
    @Setter
    private Integer dailyTargetOfNewNotes = 5;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    @Getter @Setter private UserEntity userEntity;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "note_id", referencedColumnName = "id")
    private NoteEntity noteEntity;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "notebook_id", referencedColumnName = "id")
    @Getter private NotebookEntity notebookEntity;

    public String getTitle() {
        return notebookEntity.getHeadNoteEntity().getTitle();
    }

    public NoteContentEntity getNoteContent() {
        return notebookEntity.getHeadNoteEntity().getNoteContent();
    }

    public void setNotebookEntity(NotebookEntity notebookEntity) {
        this.notebookEntity = notebookEntity;
        this.noteEntity = notebookEntity.getHeadNoteEntity();
    }

    public NoteEntity getHeadNoteEntity() {
        return notebookEntity.getHeadNoteEntity();
    }
}
