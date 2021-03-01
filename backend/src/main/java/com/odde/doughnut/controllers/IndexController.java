package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUser;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.entities.repositories.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
public class IndexController {
    private final NoteRepository noteRepository;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;

    public IndexController(NoteRepository noteRepository, UserRepository userRepository, CurrentUser currentUser) {
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
        this.currentUser = currentUser;
    }

    @GetMapping("/")
    public String home(Principal principal, Model model) {
        if (principal == null) {
            model.addAttribute("totalNotes", noteRepository.count());
            return "ask_to_login";
        }

        if (currentUser.getUser() == null) {
            model.addAttribute("user", new User());
            return "register";
        }

        return "index";
    }

}
