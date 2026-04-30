package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.WikiTitle;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteWikiTitleCache;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteWikiTitleCacheRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WikiTitleCacheService {

  private final WikiLinkResolver wikiLinkResolver;
  private final NoteWikiTitleCacheRepository noteWikiTitleCacheRepository;
  private final AuthorizationService authorizationService;

  public WikiTitleCacheService(
      WikiLinkResolver wikiLinkResolver,
      NoteWikiTitleCacheRepository noteWikiTitleCacheRepository,
      AuthorizationService authorizationService) {
    this.wikiLinkResolver = wikiLinkResolver;
    this.noteWikiTitleCacheRepository = noteWikiTitleCacheRepository;
    this.authorizationService = authorizationService;
  }

  public List<WikiTitle> wikiTitlesForViewer(Note focusNote, User viewer) {
    List<WikiTitle> out = new ArrayList<>();
    for (NoteWikiTitleCache row :
        noteWikiTitleCacheRepository.findByNote_IdOrderByIdAsc(focusNote.getId())) {
      Note target = row.getTargetNote();
      Notebook notebook =
          target.getNotebook() != null ? target.getNotebook() : focusNote.getNotebook();
      if (!authorizationService.userMayReadNotebook(viewer, notebook)) {
        continue;
      }
      out.add(new WikiTitle(row.getLinkText(), notebook.getId(), target.getSlug()));
    }
    return List.copyOf(out);
  }

  @Transactional
  public void refreshForNote(Note note, User viewer) {
    noteWikiTitleCacheRepository.deleteByNote_Id(note.getId());
    for (WikiLinkResolver.ResolvedWikiLink link :
        wikiLinkResolver.resolveWikiLinksForCache(note, viewer)) {
      NoteWikiTitleCache row = new NoteWikiTitleCache();
      row.setNote(note);
      row.setTargetNote(link.targetNote());
      row.setLinkText(link.linkText());
      noteWikiTitleCacheRepository.save(row);
    }
  }
}
