package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.services.notebookAttachment.NotebookAttachmentContent;
import java.util.List;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.springframework.stereotype.Service;

@Service
class NotebookGitCandidateAdmission {

  private final NotebookGitAcceptedRepositoryStore repositoryStore;
  private final NotebookAttachmentContent notebookAttachmentContent;
  private final NotebookGitBookSourceFileProtection bookSourceFileProtection;

  NotebookGitCandidateAdmission(
      NotebookGitAcceptedRepositoryStore repositoryStore,
      NotebookAttachmentContent notebookAttachmentContent,
      NotebookGitBookSourceFileProtection bookSourceFileProtection) {
    this.repositoryStore = repositoryStore;
    this.notebookAttachmentContent = notebookAttachmentContent;
    this.bookSourceFileProtection = bookSourceFileProtection;
  }

  List<NotebookGitProposalTreeShape.InspectedRegularFile> admit(
      NotebookGitBinding binding, Repository repository, ObjectId proposedHead) {
    ObjectId acceptedHead = ObjectId.fromString(binding.getAcceptedGitObjectId());
    List<NotebookGitProposalTreeShape.InspectedRegularFile> files =
        NotebookGitProposalTreeShape.inspectRegularFiles(repository, acceptedHead, proposedHead);
    NotebookGitProposalTreeShape.refuseMiscasedMarkdown(files);
    NotebookGitProposalTreeShape.refuseLeftoverFolderMarkers(files);
    NotebookGitProposalMarkdownFormat.assertValidTypedMarkdown(repository, files);
    try (var accepted = repositoryStore.open(binding)) {
      NotebookGitAttachmentSizeAdmission.admit(
          repository,
          proposedHead,
          accepted.repository(),
          acceptedHead,
          binding.getNotebook().getId(),
          notebookAttachmentContent);
    }
    bookSourceFileProtection.refuseChanging(binding.getNotebook().getId(), files);
    return files;
  }
}
