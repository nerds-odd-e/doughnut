package com.odde.doughnut.controllers;

import com.odde.doughnut.models.Note;
import com.odde.doughnut.models.User;
import com.odde.doughnut.repositories.NoteRepository;
import com.odde.doughnut.repositories.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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


    @GetMapping("/review")
    public String review(Principal principal, Model model) {
        User user = userRepository.findByExternalIdentifier(principal.getName());
        List<Note> notes = user.getNotesInDescendingOrder();
        model.addAttribute("notes", notes);
        return "review";
    }

    @GetMapping("/view")
    public String view(Principal principal, Model model) {
        if (principal == null) {
            model.addAttribute("totalNotes", noteRepository.count());
            return "redirect:/";
        }

        User user = userRepository.findByExternalIdentifier(principal.getName());
        model.addAttribute("notes", user.getNotes());
        return "view";
    }

    @GetMapping("/link/{id}")
    public String link(Principal principal, Model model, @PathVariable("id") String id) {
        if (principal == null) {
            model.addAttribute("totalNotes", noteRepository.count());
            return "redirect:/";
        }

        User user = userRepository.findByExternalIdentifier(principal.getName());
        Optional<Note> sourceNote = noteRepository.findById(Integer.valueOf(id));
        List<Note> linkableNotes = getLinkableNotes(user, sourceNote);
        model.addAttribute("linkableNotes", linkableNotes);
        model.addAttribute("sourceNote", sourceNote.get());
        return "link";
    }

    private List<Note> getLinkableNotes(User user, Optional<Note> sourceNote) {
        List<Note> targetNotes = sourceNote.get().getTargetNotes();
        List<Note> allNotes = user.getNotes();
        List<Note> linkableNotes = allNotes.stream()
                .filter(i -> !targetNotes.contains(i))
                .filter(i -> i != sourceNote.get())
                .collect(Collectors.toList());
        return linkableNotes;
    }
}
