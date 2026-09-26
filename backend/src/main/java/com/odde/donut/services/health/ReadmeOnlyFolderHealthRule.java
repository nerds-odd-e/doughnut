package com.odde.donut.services.health;

import com.odde.donut.controllers.dto.HealthFindingGroup;
import com.odde.donut.controllers.dto.HealthSeverity;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.repositories.FolderRepository;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ReadmeOnlyFolderHealthRule implements HealthRule {
  private final FolderRepository folderRepository;

  public ReadmeOnlyFolderHealthRule(FolderRepository folderRepository) {
    this.folderRepository = folderRepository;
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
        folderRepository.findOccupiedFolderIdsByNotebookId(notebook.getId());

    HealthFindingGroup group = findingGroup();
    group.setItems(
        FolderSubtreeOccupancy.noteEmptyFolderItems(
            folders, occupiedFolderIds, readme -> !FolderSubtreeOccupancy.isBlankReadme(readme)));
    return group;
  }
}
