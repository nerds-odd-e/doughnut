package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUser;
import com.odde.doughnut.models.Note;
import com.odde.doughnut.repositories.BazaarNoteRepository;
import com.odde.doughnut.repositories.NoteRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class BazaarController {
    private final CurrentUser currentUser;
    private final NoteRepository noteRepository;
    private final BazaarNoteRepository bazaarRepository;

    public BazaarController(CurrentUser currentUser, NoteRepository noteRepository, BazaarNoteRepository bazaarRepository) {
        this.currentUser = currentUser;
        this.noteRepository = noteRepository;
        this.bazaarRepository = bazaarRepository;
    }

    @GetMapping("/bazaar")
    public String bazaar(Model model) {
        model.addAttribute("notes", bazaarRepository.findAll());
        return "bazaar";
    }
}
