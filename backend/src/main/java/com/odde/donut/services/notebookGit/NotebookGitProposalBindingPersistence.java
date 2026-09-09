package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.factoryServices.EntityPersister;
import java.sql.Timestamp;
import org.springframework.stereotype.Service;

@Service
class NotebookGitProposalBindingPersistence {

  private final EntityPersister entityPersister;

  NotebookGitProposalBindingPersistence(EntityPersister entityPersister) {
    this.entityPersister = entityPersister;
  }

  String accept(
      NotebookGitBinding binding,
      NotebookGitProposalImporter.ImportedProposal proposal,
      Timestamp publishedAt) {
    NotebookGitBundleWriter.BundleWriteResult written =
        NotebookGitBundleWriter.write(proposal.repository());
    binding.setAcceptedGitObjectId(written.headObjectId());
    binding.setBundleBytes(written.bundleBytes());
    binding.setUpdatedAt(publishedAt);
    entityPersister.save(binding);
    return written.headObjectId();
  }
}
