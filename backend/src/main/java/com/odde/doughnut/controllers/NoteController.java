package com.odde.doughnut.controllers;

import com.odde.doughnut.models.Note;
import com.odde.doughnut.models.User;
import com.odde.doughnut.repositories.NoteRepository;
import com.odde.doughnut.repositories.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.view.RedirectView;

import java.security.Principal;

@Controller
public class NoteController {
    private final NoteRepository noteRepository;
    private final UserRepository userRepository;

    public NoteController(NoteRepository noteRepository, UserRepository userRepository) {
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/note")
    public RedirectView createNote(Principal principal, Note note, Model model) throws Exception {
        User currentUser = userRepository.findByExternalIdentifier(principal.getName());
        if (currentUser == null) throw new Exception("User does not exist");
        note.setUser(currentUser);
        noteRepository.save(note);
        return new RedirectView("/review");
    }
}
