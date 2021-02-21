package com.odde.doughnut.controllers;

import com.odde.doughnut.models.Note;
import com.odde.doughnut.models.User;
import com.odde.doughnut.repositories.NoteRepository;
import com.odde.doughnut.repositories.UserRepository;
import com.odde.doughnut.services.LinkService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;

@RestController
public class NoteRestController {
    private final NoteRepository noteRepository;
    private final UserRepository userRepository;
    private final LinkService linkService;

    public NoteRestController(NoteRepository noteRepository, UserRepository userRepository, LinkService linkService) {
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
        this.linkService = linkService;
    }

    @PostMapping("/note")
    public RedirectView createNote(@RequestAttribute("currentUser") User currentUser, Note note) {
        User user = userRepository.findByExternalIdentifier(currentUser.getExternalIdentifier());
        note.setUser(user);
        noteRepository.save(note);
        return new RedirectView("/review");
    }

    @GetMapping(value="/getNotes", produces=MediaType.APPLICATION_JSON_VALUE)
    public List<Note> getNotes(@RequestAttribute("currentUser") User currentUser) throws Exception {
        return currentUser.getNotesInDescendingOrder();
    }

    @PostMapping(value = "/linkNote", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public RedirectView linkNote(Integer sourceNoteId, Integer targetNoteId) {
        Note sourceNote = noteRepository.findById(sourceNoteId).get();
        Note targetNote = noteRepository.findById(targetNoteId).get();

        linkService.linkNote(sourceNote, targetNote);
        return new RedirectView("/review");
    }
}
