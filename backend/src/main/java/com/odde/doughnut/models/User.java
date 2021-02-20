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
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
  @Getter @Setter private String name;
  @Column(name="external_identifier")
  @Getter @Setter private String externalIdentifier;



  @OneToMany(mappedBy="user")
  @Getter @Setter private List<Note> notes = new ArrayList<>();

    public static List<Note> getFilteredLinkableNotes(List<Note> notes, String searchTerm) {
      List<Note> filteredNotes = notes.stream()
              .filter(note -> note.getTitle().contains(searchTerm))
              .collect(Collectors.toList());
      return filteredNotes;
  }


    public List<Note> getNotesInDescendingOrder() {
    List<Note> notes = getNotes();
    notes.sort(Comparator.comparing(Note::getUpdatedDatetime).reversed());
    return notes;
  }

  public List<Note> getLinkablNotes(Note source) {
      List<Note> targetNotes = source.getTargetNotes();
      List<Note> allNotes = getNotes();
      List<Note> linkableNotes = allNotes.stream()
              .filter(i -> !targetNotes.contains(i))
              .filter(i -> i.getId() != source.getId())
              .collect(Collectors.toList());
      return linkableNotes;
  }

    public List<Note> filterLinkableNotes(Note source, String searchTerm) {
        List<Note> linkableNotes = getLinkablNotes(source);
        if(searchTerm != null) {
            linkableNotes = getFilteredLinkableNotes(linkableNotes, searchTerm);
        }
        return linkableNotes;
    }
}
