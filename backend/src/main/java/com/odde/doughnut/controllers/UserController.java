package com.odde.doughnut.controllers;

import com.odde.doughnut.models.User;
import com.odde.doughnut.repositories.NoteRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.security.Principal;

@Controller
public class UserController {
  private final NoteRepository noteRepository;

  public UserController(NoteRepository noteRepository) {

    this.noteRepository = noteRepository;
  }

  @PostMapping("/users")
  public String createUser(Principal principal, User user, Model model) {
    return "index";
  }
}
