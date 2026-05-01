package com.odde.doughnut.services;

import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.utils.WikiSlugGeneration;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class WikiSlugPathAssignment {

  private WikiSlugPathAssignment() {}

  public static void setFolderSlug(Folder folder, Set<String> siblingBasenames) {
    String basename = WikiSlugGeneration.uniqueSlugWithin(folder.getName(), siblingBasenames);
    Folder parent = folder.getParentFolder();
    if (parent == null) {
      folder.setSlug(basename);
      return;
    }
    folder.setSlug(parent.getSlug() + "/" + basename);
  }

  public static void setNoteSlug(Note note, Set<String> siblingBasenames) {
    setNoteSlug(note, siblingBasenames, Note.MAX_SLUG_LENGTH);
  }

  public static void setNoteSlug(Note note, Set<String> siblingBasenames, int maxSlugLength) {
    String titleSource = Objects.toString(note.getTitle(), "");
    String basename = WikiSlugGeneration.uniqueSlugWithin(titleSource, siblingBasenames);
    Folder folder = note.getFolder();
    if (folder == null) {
      if (basename.length() <= maxSlugLength) {
        note.setSlug(basename);
        return;
      }
      note.setSlug(
          WikiSlugGeneration.uniqueSlugWithinMaxLen(
              WikiSlugGeneration.toBaseSlug(titleSource), siblingBasenames, maxSlugLength));
      return;
    }
    String folderSlug = folder.getSlug();
    String full = folderSlug + "/" + basename;
    if (full.length() <= maxSlugLength) {
      note.setSlug(full);
      return;
    }
    Integer id = note.getId();
    String seed =
        id != null
            ? WikiSlugGeneration.toBaseSlug(titleSource) + "-" + id
            : WikiSlugGeneration.toBaseSlug(titleSource);
    int maxBasename = maxSlugLength - folderSlug.length() - 1;
    if (maxBasename < 1) {
      throw new IllegalStateException(
          "Folder slug leaves no room for a note basename; use notebook-scoped fallback");
    }
    basename = WikiSlugGeneration.uniqueSlugWithinMaxLen(seed, siblingBasenames, maxBasename);
    note.setSlug(folderSlug + "/" + basename);
  }

  public static Set<String> basenamesFromSlugs(List<String> slugs) {
    Set<String> set = new HashSet<>();
    for (String s : slugs) {
      if (s.isEmpty()) {
        continue;
      }
      set.add(basenameOf(s));
    }
    return set;
  }

  public static String basenameOf(String slug) {
    int i = slug.lastIndexOf('/');
    return i < 0 ? slug : slug.substring(i + 1);
  }
}
