package com.odde.donut.services.health;

import com.odde.donut.algorithms.FrontmatterAliases;
import com.odde.donut.controllers.dto.HealthFindingGroup;
import com.odde.donut.controllers.dto.HealthFindingItem;
import com.odde.donut.controllers.dto.HealthSeverity;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.repositories.NoteRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PipeNameCompatibilityHealthRule implements HealthRule {
  private static final String TITLE_WARNING =
      "The title contains a pipe, so its filename is not Windows-compatible and references may not work in other Markdown tools.";
  private static final String ALIAS_WARNING =
      "An alias contains a pipe, so references may not work in other Markdown tools.";
  private static final String TITLE_AND_ALIAS_WARNING =
      "The title and an alias contain pipes, so its filename is not Windows-compatible and references may not work in other Markdown tools.";

  private final NoteRepository noteRepository;

  public PipeNameCompatibilityHealthRule(NoteRepository noteRepository) {
    this.noteRepository = noteRepository;
  }

  @Override
  public String id() {
    return HealthRuleIds.PIPE_NAME_COMPATIBILITY;
  }

  @Override
  public String title() {
    return "Pipe compatibility";
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
    List<HealthFindingItem> items = new ArrayList<>();
    for (Note note : noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId())) {
      boolean pipeTitle = note.getTitle().contains("|");
      boolean pipeAlias =
          FrontmatterAliases.fromNoteContent(note.getContent()).stream()
              .anyMatch(alias -> alias.contains("|"));
      if (!pipeTitle && !pipeAlias) {
        continue;
      }

      HealthFindingItem item = new HealthFindingItem();
      item.setNoteId(note.getId());
      item.setLabel(note.getTitle());
      item.setMessage(warningFor(pipeTitle, pipeAlias));
      items.add(item);
    }

    HealthFindingGroup group = findingGroup();
    group.setItems(items);
    return group;
  }

  private static String warningFor(boolean pipeTitle, boolean pipeAlias) {
    if (pipeTitle && pipeAlias) {
      return TITLE_AND_ALIAS_WARNING;
    }
    return pipeTitle ? TITLE_WARNING : ALIAS_WARNING;
  }
}
