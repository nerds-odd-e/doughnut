package com.odde.doughnut.services;

import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.repositories.FolderRepository;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.utils.WikiSlugGeneration;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class WikiSlugPathService {

  private final FolderRepository folderRepository;
  private final NoteRepository noteRepository;

  public WikiSlugPathService(FolderRepository folderRepository, NoteRepository noteRepository) {
    this.folderRepository = folderRepository;
    this.noteRepository = noteRepository;
  }

  public void assignSlugForNewFolder(Folder folder) {
    Integer notebookId = folder.getNotebook().getId();
    Integer parentFolderId =
        folder.getParentFolder() == null ? null : folder.getParentFolder().getId();
    List<String> siblingSlugs =
        folderRepository.findSlugsOfSiblingFolders(notebookId, parentFolderId);
    Set<String> siblingBasenames = basenamesFromSlugs(siblingSlugs);
    String basename = WikiSlugGeneration.uniqueSlugWithin(folder.getName(), siblingBasenames);
    Folder parent = folder.getParentFolder();
    if (parent == null) {
      folder.setSlug(basename);
      return;
    }
    String parentSlug = parent.getSlug();
    if (parentSlug == null || parentSlug.isEmpty()) {
      folder.setSlug(basename);
    } else {
      folder.setSlug(parentSlug + "/" + basename);
    }
  }

  public void assignSlugForNewNote(Note note) {
    Integer notebookId = note.getNotebook().getId();
    Folder folder = note.getFolder();
    Integer folderId = folder == null ? null : folder.getId();
    Integer excludeNoteId = note.getId();
    List<String> siblingSlugs =
        noteRepository.findSlugsOfNotesInFolderScope(notebookId, folderId, excludeNoteId);
    Set<String> siblingBasenames = basenamesFromSlugs(siblingSlugs);
    String titleSource = Objects.toString(note.getTitle(), "");
    String basename = WikiSlugGeneration.uniqueSlugWithin(titleSource, siblingBasenames);
    if (folder == null) {
      note.setSlug(basename);
      return;
    }
    String folderSlug = folder.getSlug();
    if (folderSlug == null || folderSlug.isEmpty()) {
      note.setSlug(basename);
    } else {
      note.setSlug(folderSlug + "/" + basename);
    }
  }

  private static Set<String> basenamesFromSlugs(List<String> slugs) {
    Set<String> set = new HashSet<>();
    for (String s : slugs) {
      if (s == null || s.isEmpty()) {
        continue;
      }
      set.add(basenameOf(s));
    }
    return set;
  }

  static String basenameOf(String slug) {
    int i = slug.lastIndexOf('/');
    return i < 0 ? slug : slug.substring(i + 1);
  }
}
