package com.odde.doughnut.services.health;

import com.odde.doughnut.controllers.dto.HealthFindingItem;
import com.odde.doughnut.entities.Folder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

final class FolderSubtreeLiveNotes {
  private FolderSubtreeLiveNotes() {}

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

  static boolean isBlankReadme(String readmeContent) {
    return readmeContent == null || readmeContent.isBlank();
  }

  static List<HealthFindingItem> noteEmptyFolderItems(
      List<Folder> folders,
      Set<Integer> occupiedFolderIds,
      Predicate<String> readmeContentMatches) {
    Map<Integer, List<Folder>> childrenByParentId = childrenByParentId(folders);
    Map<Integer, Boolean> memo = new HashMap<>();
    List<HealthFindingItem> items = new ArrayList<>();
    for (Folder folder : folders) {
      if (subtreeHasLiveNotes(folder, childrenByParentId, occupiedFolderIds, memo)) {
        continue;
      }
      if (!readmeContentMatches.test(folder.getReadmeContent())) {
        continue;
      }
      HealthFindingItem item = new HealthFindingItem();
      item.setFolderId(folder.getId());
      item.setLabel(folder.getName());
      items.add(item);
    }
    return items;
  }

  static List<Folder> cascadeSafeFullyEmptyFolders(
      List<Folder> folders, Set<Integer> occupiedFolderIds) {
    Map<Integer, List<Folder>> childrenByParentId = childrenByParentId(folders);
    Set<Integer> emptyIds = fullyEmptyFolderIds(folders, occupiedFolderIds, childrenByParentId);
    List<Folder> deletable = new ArrayList<>();
    for (Folder folder : folders) {
      if (!emptyIds.contains(folder.getId())) {
        continue;
      }
      if (!allDescendantsInEmptySet(folder, emptyIds, childrenByParentId)) {
        continue;
      }
      deletable.add(folder);
    }
    deletable.sort(Comparator.comparingInt(FolderSubtreeLiveNotes::folderDepth).reversed());
    return deletable;
  }

  private static Set<Integer> fullyEmptyFolderIds(
      List<Folder> folders,
      Set<Integer> occupiedFolderIds,
      Map<Integer, List<Folder>> childrenByParentId) {
    Map<Integer, Boolean> memo = new HashMap<>();
    Set<Integer> emptyIds = new HashSet<>();
    for (Folder folder : folders) {
      if (subtreeHasLiveNotes(folder, childrenByParentId, occupiedFolderIds, memo)) {
        continue;
      }
      if (!isBlankReadme(folder.getReadmeContent())) {
        continue;
      }
      emptyIds.add(folder.getId());
    }
    return emptyIds;
  }

  private static boolean allDescendantsInEmptySet(
      Folder folder, Set<Integer> emptyIds, Map<Integer, List<Folder>> childrenByParentId) {
    for (Folder child : childrenByParentId.getOrDefault(folder.getId(), List.of())) {
      if (!emptyIds.contains(child.getId())) {
        return false;
      }
      if (!allDescendantsInEmptySet(child, emptyIds, childrenByParentId)) {
        return false;
      }
    }
    return true;
  }

  private static int folderDepth(Folder folder) {
    int depth = 0;
    Folder current = folder.getParentFolder();
    while (current != null) {
      depth++;
      current = current.getParentFolder();
    }
    return depth;
  }
}
