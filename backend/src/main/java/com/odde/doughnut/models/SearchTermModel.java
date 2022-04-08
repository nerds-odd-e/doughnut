package com.odde.doughnut.models;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.json.SearchTerm;
import com.odde.doughnut.entities.repositories.NoteRepository;
import org.apache.logging.log4j.util.Strings;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SearchTermModel {
    private final User user;
    private final SearchTerm searchTerm;
    NoteRepository noteRepository;

    public SearchTermModel(User entity, NoteRepository noteRepository, SearchTerm searchTerm) {
        this.user = entity;
        this.searchTerm = searchTerm;
        this.noteRepository = noteRepository;
    }

    private List<Note> search() {
        if (searchTerm.getAllMyCircles()) {
            return noteRepository.searchForUserInAllMyNotebooksSubscriptionsAndCircle(user, getPattern());
        }
        if (searchTerm.getAllMyNotebooksAndSubscriptions()) {
            return noteRepository.searchForUserInAllMyNotebooksAndSubscriptions(user, getPattern());
        }
        Notebook notebook = searchTerm.note.map(Note::getNotebook).orElse(null);
        return noteRepository.searchInNotebook(notebook, getPattern());
    }

    private String getPattern() {
        return Pattern.quote(searchTerm.getTrimmedSearchKey());
    }

    public List<Note> searchForNotes() {
        if (Strings.isBlank(searchTerm.getTrimmedSearchKey())) {
            return List.of();
        }

        Integer avoidNoteId = searchTerm.note.map(Note::getId).orElse(null);
        return search().stream().filter(n -> !n.getId().equals(avoidNoteId)).collect(Collectors.toList());
    }
}
