package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUser;
import com.odde.doughnut.controllers.exceptions.NoAccessRightException;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.repositories.NoteRepository;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;

@RestController
public class NoteRestController {
    private final NoteRepository noteRepository;
    private final CurrentUser currentUser;

    public NoteRestController(NoteRepository noteRepository, CurrentUser currentUser) {
        this.noteRepository = noteRepository;
        this.currentUser = currentUser;
    }

    @GetMapping(value = "/api/notes", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Note> getNotes() {
        return currentUser.getUser().getNotesInDescendingOrder();
    }

    @PostMapping(value = "/notes/{id}/delete")
    public RedirectView deleteNote(@PathVariable("id") Integer noteId) throws NoAccessRightException {
        Note note = noteRepository.findById(noteId).get();
        currentUser.getUser().checkAuthorization(note);
        noteRepository.delete(note);
        return new RedirectView("/notes");
    }

}
