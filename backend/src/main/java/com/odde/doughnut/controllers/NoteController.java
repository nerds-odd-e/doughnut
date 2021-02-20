package com.odde.doughnut.controllers;

import com.odde.doughnut.models.Note;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
public class NoteController {
    @GetMapping("/note")
    public String notes(Principal principal, Model model) {
        model.addAttribute("note", new Note());
        return "note";
    }
}
