package com.odde.doughnut.services.health;

import com.odde.doughnut.controllers.dto.HealthFindingGroup;
import com.odde.doughnut.controllers.dto.HealthFindingItem;
import com.odde.doughnut.controllers.dto.HealthSeverity;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.repositories.FolderRepository;
import com.odde.doughnut.entities.repositories.NoteRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    Map<Integer, List<Folder>> childrenByParentId = childrenByParentId(folders);
    Map<Integer, Boolean> subtreeHasLiveNotes = new HashMap<>();

    List<HealthFindingItem> items = new ArrayList<>();
    for (Folder folder : folders) {
      if (!subtreeHasLiveNotes(folder, childrenByParentId, occupiedFolderIds, subtreeHasLiveNotes)
          && isBlankReadme(folder.getReadmeContent())) {
        HealthFindingItem item = new HealthFindingItem();
        item.setFolderId(folder.getId());
        item.setLabel(folder.getName());
        items.add(item);
      }
    }

    HealthFindingGroup group = new HealthFindingGroup();
    group.setRuleId(id());
    group.setTitle(title());
    group.setSeverity(severity());
    group.setAutoFixable(autoFixable());
    group.setItems(items);
    return group;
  }

  private static Map<Integer, List<Folder>> childrenByParentId(List<Folder> folders) {
    Map<Integer, List<Folder>> childrenByParentId = new HashMap<>();
    for (Folder folder : folders) {
      Integer parentId = folder.getParentFolder() == null ? null : folder.getParentFolder().getId();
      childrenByParentId.computeIfAbsent(parentId, ignored -> new ArrayList<>()).add(folder);
    }
    return childrenByParentId;
  }

  private static boolean subtreeHasLiveNotes(
      Folder folder,
      Map<Integer, List<Folder>> childrenByParentId,
      Set<Integer> occupiedFolderIds,
      Map<Integer, Boolean> memo) {
    Boolean cached = memo.get(folder.getId());
    if (cached != null) {
      return cached;
    }
    if (occupiedFolderIds.contains(folder.getId())) {
      memo.put(folder.getId(), true);
      return true;
    }
    for (Folder child : childrenByParentId.getOrDefault(folder.getId(), List.of())) {
      if (subtreeHasLiveNotes(child, childrenByParentId, occupiedFolderIds, memo)) {
        memo.put(folder.getId(), true);
        return true;
      }
    }
    memo.put(folder.getId(), false);
    return false;
  }

  private static boolean isBlankReadme(String readmeContent) {
    return readmeContent == null || readmeContent.isBlank();
  }
}
