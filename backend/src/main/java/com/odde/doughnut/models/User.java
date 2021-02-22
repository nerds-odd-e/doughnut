package com.odde.doughnut.models;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "user")
public class User {
    @Id @Getter @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Getter @Setter private String name;
    @Column(name = "external_identifier") @Getter @Setter private String externalIdentifier;

    @OneToMany(mappedBy = "user")
    @Getter @Setter private List<Note> notes = new ArrayList<>();

    public List<Note> getNotesInDescendingOrder() {
        List<Note> notes = getNotes();
        notes.sort(Comparator.comparing(Note::getUpdatedDatetime).reversed());
        return notes;
    }

    public List<Note> filterLinkableNotes(Note source, String searchTerm) {
        List<Note> linkableNotes = getAllLinkableNotes(source);
        if (searchTerm != null) {
            return linkableNotes.stream()
                    .filter(note -> note.getTitle().contains(searchTerm))
                    .collect(Collectors.toList());
        }
        return linkableNotes;
    }

    private List<Note> getAllLinkableNotes(Note source) {
        List<Note> targetNotes = source.getTargetNotes();
        List<Note> allNotes = getNotes();
        return allNotes.stream()
                .filter(i -> !targetNotes.contains(i))
                .filter(i -> !i.equals(source))
                .collect(Collectors.toList());
    }
}
