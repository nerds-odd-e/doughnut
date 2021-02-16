package com.odde.doughnut.controllers;

import com.odde.doughnut.models.Note;
import com.odde.doughnut.models.User;
import com.odde.doughnut.repositories.NoteRepository;
import com.odde.doughnut.repositories.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
public class IndexController {
  private final NoteRepository noteRepository;
  private final UserRepository userRepository;

  public IndexController(NoteRepository noteRepository, UserRepository userRepository) {

    this.noteRepository = noteRepository;
    this.userRepository = userRepository;
  }

  @GetMapping("/")
  public String home(Principal principal, Model model) {
    if (principal == null) {
      model.addAttribute("totalNotes", noteRepository.count());
      return "login";
    }

    User currentUser = userRepository.findByExternalIdentifier(principal.getName());
    if (currentUser == null) {
      model.addAttribute("user", new User());
      return "register";
    }

    model.addAttribute("name", principal.getName());
    model.addAttribute("user_details", principal.toString());
    model.addAttribute("currentUser", currentUser);

    return "index";
  }

  @GetMapping("/note")
  public String notes(Principal principal, Model model) {
    if (principal == null) {
      model.addAttribute("totalNotes", noteRepository.count());
      return "redirect:/";
    }
    model.addAttribute("note", new Note());
    return "note";
  }
}
