package com.odde.doughnut;

import com.odde.doughnut.repositories.NoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {
  private final NoteRepository noteRepository;

  public IndexController(NoteRepository noteRepository) {

    this.noteRepository = noteRepository;
  }

  @GetMapping("/")
  public String home(@AuthenticationPrincipal OAuth2User user, Model model) {
    if (user == null) {
      model.addAttribute("totalNotes", noteRepository.count());
      return "login";
    }
    model.addAttribute("name", user.getAttribute("name"));
    model.addAttribute("user_details", user.toString());
    return "index";
  }
}
