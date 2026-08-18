package com.odde.doughnut.services.health;

import com.odde.doughnut.controllers.dto.HealthFindingGroup;
import com.odde.doughnut.controllers.dto.HealthFindingItem;
import com.odde.doughnut.controllers.dto.HealthSeverity;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.repositories.NoteRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class OkfIncompatibleTitleHealthRule implements HealthRule {
  private static final Set<String> OKF_INCOMPATIBLE_TITLES =
      Set.of("index", "index.md", "log", "log.md");

  private final NoteRepository noteRepository;

  public OkfIncompatibleTitleHealthRule(NoteRepository noteRepository) {
    this.noteRepository = noteRepository;
  }

  @Override
  public String id() {
    return HealthRuleIds.OKF_INCOMPATIBLE_TITLES;
  }

  @Override
  public String title() {
    return "OKF-incompatible titles";
  }

  @Override
  public HealthSeverity severity() {
    return HealthSeverity.warning;
  }

  @Override
  public boolean autoFixable() {
    return false;
  }

  @Override
  public HealthFindingGroup evaluate(Notebook notebook, HealthRunContext context) {
    List<Note> liveNotes = noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId());
    List<HealthFindingItem> items = new ArrayList<>();
    for (Note note : liveNotes) {
      if (!isOkfIncompatibleTitle(note.getTitle())) {
        continue;
      }
      HealthFindingItem item = new HealthFindingItem();
      item.setNoteId(note.getId());
      item.setLabel(note.getTitle());
      items.add(item);
    }

    HealthFindingGroup group = findingGroup();
    group.setItems(items);
    return group;
  }

  private static boolean isOkfIncompatibleTitle(String title) {
    return OKF_INCOMPATIBLE_TITLES.contains(title.trim().toLowerCase(Locale.ROOT));
  }
}
