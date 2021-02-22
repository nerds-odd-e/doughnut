package com.odde.doughnut.controllers;

import com.odde.doughnut.models.Note;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class ReviewController {
    private final CurrentUser currentUser;

    public ReviewController(CurrentUser currentUser) {
        this.currentUser = currentUser;
    }

    @GetMapping("/review")
    public String review(Model model) {
        List<Note> notes = currentUser.getUser().getNotesInDescendingOrder();
        model.addAttribute("notes", notes);
        return "review";
    }
}
