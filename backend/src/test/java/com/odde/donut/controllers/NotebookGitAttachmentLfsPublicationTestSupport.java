package com.odde.donut.controllers;

import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitAttachmentRepresentation;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.FolderRepository;
import org.springframework.beans.factory.annotation.Autowired;

/** Shared LFS-binding and pointer fixtures for attachment publication controller proof. */
abstract class NotebookGitAttachmentLfsPublicationTestSupport
    extends NotebookGitAttachmentSizeAdmissionTestSupport {

  @Autowired FolderRepository folderRepository;

  Notebook enableLfs(Notebook notebook) {
    NotebookGitBinding binding = reloadCommittedBinding(notebook.getId());
    binding.setAttachmentRepresentation(NotebookGitAttachmentRepresentation.LFS);
    notebookGitBindingRepository.save(binding);
    return notebook;
  }
}
