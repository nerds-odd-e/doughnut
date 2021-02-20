package com.odde.doughnut.controllers;

import com.odde.doughnut.models.Note;
import com.odde.doughnut.models.User;
import com.odde.doughnut.repositories.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
public class NoteController {
    private final UserRepository userRepository;

    public NoteController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/note")
    public String notes(Principal principal, Model model) {
        model.addAttribute("note", new Note());
        return "note";
    }

    @GetMapping("/all_my_notes")
    public String all_my_notes(Principal principal, Model model) {
        User user = userRepository.findByExternalIdentifier(principal.getName());
        model.addAttribute("all_my_notes", user.getNotes());
        return "view";
    }

}
