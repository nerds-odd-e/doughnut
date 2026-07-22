package com.odde.doughnut.services.health;

import com.odde.doughnut.controllers.dto.HealthFindingGroup;
import com.odde.doughnut.controllers.dto.HealthSeverity;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.repositories.FolderRepository;
import com.odde.doughnut.entities.repositories.NoteRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ReadmeOnlyFolderHealthRule implements HealthRule {
  private final FolderRepository folderRepository;
  private final NoteRepository noteRepository;

  public ReadmeOnlyFolderHealthRule(
      FolderRepository folderRepository, NoteRepository noteRepository) {
    this.folderRepository = folderRepository;
    this.noteRepository = noteRepository;
  }

  @Override
  public String id() {
    return HealthRuleIds.README_ONLY_FOLDERS;
  }

  @Override
  public String title() {
    return "Readme-only folders";
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
    List<Folder> folders = folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId());
    Set<Integer> occupiedFolderIds =
        new HashSet<>(noteRepository.findLiveNoteFolderIdsByNotebookId(notebook.getId()));

    HealthFindingGroup group = new HealthFindingGroup();
    group.setRuleId(id());
    group.setTitle(title());
    group.setSeverity(severity());
    group.setAutoFixable(autoFixable());
    group.setItems(
        FolderSubtreeLiveNotes.noteEmptyFolderItems(
            folders, occupiedFolderIds, readme -> !FolderSubtreeLiveNotes.isBlankReadme(readme)));
    return group;
  }
}
