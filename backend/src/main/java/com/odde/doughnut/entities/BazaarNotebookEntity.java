package com.odde.doughnut.entities;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "bazaar_notebook")
public class BazaarNotebookEntity {
    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;

    @ManyToOne
    @JoinColumn(name = "note_id", referencedColumnName = "id")
    private NoteEntity note;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "notebook_id", referencedColumnName = "id")
    @Getter private NotebookEntity notebookEntity;

    public void setNotebookEntity(NotebookEntity notebookEntity) {

        this.notebookEntity = notebookEntity;
        this.note = notebookEntity.getHeadNoteEntity();
    }
}
