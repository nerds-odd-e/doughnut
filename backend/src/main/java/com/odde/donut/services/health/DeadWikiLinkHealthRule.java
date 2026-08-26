package com.odde.donut.services.health;

import com.odde.donut.controllers.dto.HealthFindingGroup;
import com.odde.donut.controllers.dto.HealthFindingItem;
import com.odde.donut.controllers.dto.HealthSeverity;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.services.WikiLinkResolver;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DeadWikiLinkHealthRule implements HealthRule {
  private final NoteRepository noteRepository;
  private final WikiLinkResolver wikiLinkResolver;

  public DeadWikiLinkHealthRule(NoteRepository noteRepository, WikiLinkResolver wikiLinkResolver) {
    this.noteRepository = noteRepository;
    this.wikiLinkResolver = wikiLinkResolver;
  }

  @Override
  public String id() {
    return HealthRuleIds.DEAD_WIKI_LINKS;
  }

  @Override
  public String title() {
    return "Dead wiki links";
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
    List<HealthFindingGroup> children = new ArrayList<>();
    for (Note note : liveNotes) {
      List<String> deadTokens = wikiLinkResolver.unresolvedWikiLinkTokens(note, context.viewer());
      if (deadTokens.isEmpty()) {
        continue;
      }
      children.add(childGroupForNote(note, deadTokens));
    }

    HealthFindingGroup group = findingGroup();
    group.setItems(List.of());
    group.setChildren(children);
    return group;
  }

  private HealthFindingGroup childGroupForNote(Note note, List<String> deadTokens) {
    HealthFindingGroup child = findingGroup();
    child.setTitle(note.getTitle());
    List<HealthFindingItem> items = new ArrayList<>();
    for (String token : deadTokens) {
      HealthFindingItem item = new HealthFindingItem();
      item.setNoteId(note.getId());
      item.setWikiLinkToken(token);
      item.setLabel(token);
      items.add(item);
    }
    child.setItems(items);
    return child;
  }
}
