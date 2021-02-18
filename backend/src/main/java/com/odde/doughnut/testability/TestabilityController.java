
package com.odde.doughnut.testability;

import com.odde.doughnut.models.Note;
import com.odde.doughnut.models.User;
import com.odde.doughnut.repositories.NoteRepository;
import com.odde.doughnut.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityManagerFactory;
import java.security.Principal;



@RestController
@RequestMapping("/api/testability")
class TestabilityController {
  @Autowired
  EntityManagerFactory emf;

  @Autowired
  NoteRepository noteRepository;

  @Autowired
  UserRepository userRepository;

  @GetMapping("/clean_db")
  public String cleanDB() {
    new DBCleanerWorker(emf).truncateAllTables();
    return "OK";
  }

  @PostMapping("/seed_note")
  public int seedNote(Principal principal, @RequestBody Note note) throws Exception {
    User currentUser = userRepository.findByExternalIdentifier(principal.getName());
    if (currentUser == null) throw new Exception("User does not exist");
    note.setUser(currentUser);
    noteRepository.save(note);
    return note.getId();
  }
}
