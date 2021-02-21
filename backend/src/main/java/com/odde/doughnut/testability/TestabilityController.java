
package com.odde.doughnut.testability;

import com.odde.doughnut.services.LinkService;
import com.odde.doughnut.models.Note;
import com.odde.doughnut.models.User;
import com.odde.doughnut.repositories.NoteRepository;
import com.odde.doughnut.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityManagerFactory;
import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/testability")
class TestabilityController {
  @Autowired
  EntityManagerFactory emf;

  @Autowired
  NoteRepository noteRepository;

  @Autowired
  UserRepository userRepository;

  @Autowired
  LinkService linkService;

  @GetMapping("/clean_db")
  public String cleanDB() {
    new DBCleanerWorker(emf).truncateAllTables();
    return "OK";
  }

  @PostMapping("/seed_notes")
  public List<Integer> seedNote(Principal principal, @RequestBody List<Note> notes) throws Exception {
    User currentUser = userRepository.findByExternalIdentifier(principal.getName());
    if (currentUser == null) throw new Exception("User does not exist");

    for (Note note : notes) {
      note.setUser(currentUser);
    }
    noteRepository.saveAll(notes);
    return notes.stream().map(Note::getId).collect(Collectors.toList());

  }
}
