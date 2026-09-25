package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.repositories.NotebookGitBindingRepository;
import com.odde.donut.services.NumberedNameSelection;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;

/**
 * A Donut-chosen name for a new file at a notebook's root: the preferred name when it is a plain
 * filename, otherwise the fallback, numbered before its extension until no file, note or folder in
 * the accepted tree has it. A notebook without a Git binding has no accepted tree to occupy names.
 */
@Service
public class NotebookRootFreeFilename {
  private final NotebookGitBindingRepository bindingRepository;
  private final NotebookGitAcceptedRepositoryStore repositoryStore;

  public NotebookRootFreeFilename(
      NotebookGitBindingRepository bindingRepository,
      NotebookGitAcceptedRepositoryStore repositoryStore) {
    this.bindingRepository = bindingRepository;
    this.repositoryStore = repositoryStore;
  }

  public String choose(Integer notebookId, String preferred, String fallback) {
    String filename = NotebookGitPortablePath.isPlainFilename(preferred) ? preferred : fallback;
    Predicate<String> taken =
        bindingRepository
            .findByNotebook_Id(notebookId)
            .map(repositoryStore::takenPaths)
            .orElse(path -> false);
    return NumberedNameSelection.firstAvailableFilename(filename, taken);
  }
}
