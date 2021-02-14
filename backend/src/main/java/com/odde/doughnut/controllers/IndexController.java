package com.odde.doughnut.controllers;

import com.odde.doughnut.models.User;
import com.odde.doughnut.repositories.NoteRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
public class IndexController {
  private final NoteRepository noteRepository;

  public IndexController(NoteRepository noteRepository) {

    this.noteRepository = noteRepository;
  }

  @GetMapping("/")
  public String home(Principal user, Model model) {
    if (user == null) {
      model.addAttribute("totalNotes", noteRepository.count());
      return "login";
    }
    model.addAttribute("name", user.getName());
    model.addAttribute("user_details", user.toString());

    model.addAttribute("user", new User());
    return "register";
  }
}
