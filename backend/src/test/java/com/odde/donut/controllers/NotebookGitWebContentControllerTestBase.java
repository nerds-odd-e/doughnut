package com.odde.donut.controllers;

import com.odde.donut.controllers.dto.NoteUpdateContentDTO;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.springframework.beans.factory.annotation.Autowired;

abstract class NotebookGitWebContentControllerTestBase extends NotebookGitBundleControllerTestBase {
  static final String ACCEPTED_CONTENT = "---\ntype: Note\n---\naccepted content";
  static final String EDITED_CONTENT = "---\ntype: Note\n---\nedited content";

  @Autowired TextContentController textContentController;
  @Autowired NoteController noteController;

  NotebookGitBinding binding(Notebook notebook) {
    return notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
  }

  static NoteUpdateContentDTO contentDto(String content) {
    NoteUpdateContentDTO dto = new NoteUpdateContentDTO();
    dto.setContent(content);
    return dto;
  }

  static List<String> portablePaths(InMemoryRepository repository, ObjectId head) throws Exception {
    try (RevWalk revWalk = new RevWalk(repository);
        TreeWalk treeWalk = new TreeWalk(repository)) {
      treeWalk.addTree(revWalk.parseCommit(head).getTree());
      treeWalk.setRecursive(true);
      List<String> paths = new ArrayList<>();
      while (treeWalk.next()) {
        paths.add(treeWalk.getPathString());
      }
      return paths;
    }
  }
}
