package com.odde.donut.services.health;

import com.odde.donut.controllers.dto.HealthFindingGroup;
import com.odde.donut.controllers.dto.HealthSeverity;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.entities.repositories.NoteRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class EmptyFolderHealthRule implements HealthRule {
  private final FolderRepository folderRepository;
  private final NoteRepository noteRepository;

  public EmptyFolderHealthRule(FolderRepository folderRepository, NoteRepository noteRepository) {
    this.folderRepository = folderRepository;
    this.noteRepository = noteRepository;
  }

  @Override
  public String id() {
    return HealthRuleIds.EMPTY_FOLDERS;
  }

  @Override
  public String title() {
    return "Empty folders";
  }

  @Override
  public HealthSeverity severity() {
    return HealthSeverity.warning;
  }

  @Override
  public boolean autoFixable() {
    return true;
  }

  @Override
  public HealthFindingGroup evaluate(Notebook notebook, HealthRunContext context) {
    List<Folder> folders = folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId());
    Set<Integer> occupiedFolderIds =
        new HashSet<>(noteRepository.findLiveNoteFolderIdsByNotebookId(notebook.getId()));

    HealthFindingGroup group = findingGroup();
    group.setItems(
        FolderSubtreeLiveNotes.noteEmptyFolderItems(
            folders, occupiedFolderIds, FolderSubtreeLiveNotes::isBlankReadme));
    return group;
  }
}
