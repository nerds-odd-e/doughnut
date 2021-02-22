package com.odde.doughnut.controllers;

import com.odde.doughnut.models.Note;
import com.odde.doughnut.models.User;
import com.odde.doughnut.repositories.NoteRepository;
import com.odde.doughnut.services.LinkService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;

@RestController
public class NoteRestController {
    private final NoteRepository noteRepository;
    private final LinkService linkService;
    private final ICurrentUser currentUser;

    public NoteRestController(NoteRepository noteRepository, LinkService linkService, ICurrentUser currentUser) {
        this.noteRepository = noteRepository;
        this.linkService = linkService;
        this.currentUser = currentUser;
    }

    @PostMapping("/note")
    public RedirectView createNote(Note note) {
        User user = currentUser.getUser();
        note.setUser(user);
        noteRepository.save(note);
        return new RedirectView("/review");
    }

    @GetMapping(value="/getNotes", produces=MediaType.APPLICATION_JSON_VALUE)
    public List<Note> getNotes() throws Exception {
        return currentUser.getUser().getNotesInDescendingOrder();
    }

    @PostMapping(value = "/linkNote", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public RedirectView linkNote(Integer sourceNoteId, Integer targetNoteId) {
        Note sourceNote = noteRepository.findById(sourceNoteId).get();
        Note targetNote = noteRepository.findById(targetNoteId).get();

        linkService.linkNote(sourceNote, targetNote);
        return new RedirectView("/review");
    }

}
