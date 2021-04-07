package com.odde.doughnut.entities;

import lombok.Getter;

import javax.persistence.*;

@Entity
@Table(name = "notebook_head_note")
public class NotebookHeadNoteEntity {
    @Id @Getter @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
}
