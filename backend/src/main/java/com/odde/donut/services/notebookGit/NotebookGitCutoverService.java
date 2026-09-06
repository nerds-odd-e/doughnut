package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.entities.repositories.NotebookGitBindingRepository;
import com.odde.donut.services.notebookExport.ExportFolderRow;
import com.odde.donut.services.notebookExport.ExportNoteRow;
import com.odde.donut.services.notebookExport.NotebookExportRows;
import com.odde.donut.services.notebookExport.PortableTreeEntry;
import com.odde.donut.services.notebookExport.PortableTreeSnapshot;
import com.odde.donut.services.notebookGit.NotebookGitBundleWriter.BundleWriteResult;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.eclipse.jgit.lib.Repository;
import org.springframework.stereotype.Service;

/**
 * Gives one notebook its accepted Git binding: builds the notebook's canonical Portable-tree
 * snapshot as of a given commit time, commits it as a single parentless root commit under a stable
 * Donut system identity, and persists the accepted binding.
 *
 * <p>Two callers rely on this: the fleet cutover wiring backfills a notebook that predates Git
 * backing, and {@code NotebookService} calls it at creation time so every post-cutover notebook
 * starts Git-backed from an empty tree. Neither path fabricates earlier history or lets an owner
 * opt out: the caller supplies the commit time, and any failure to build a valid tree/bundle
 * propagates rather than persisting a partial binding.
 */
@Service
public class NotebookGitCutoverService {

  /** Stable system identity used for Donut-generated Git commits such as this cutover. */
  public static final String SYSTEM_AUTHOR_NAME = "Donut System";

  public static final String SYSTEM_AUTHOR_EMAIL = "system@donut.local";

  static final String CUTOVER_COMMIT_MESSAGE =
      "Cutover: snapshot existing notebook content into Git";

  private final FolderRepository folderRepository;
  private final NoteRepository noteRepository;
  private final NotebookGitBindingRepository notebookGitBindingRepository;

  public NotebookGitCutoverService(
      FolderRepository folderRepository,
      NoteRepository noteRepository,
      NotebookGitBindingRepository notebookGitBindingRepository) {
    this.folderRepository = folderRepository;
    this.noteRepository = noteRepository;
    this.notebookGitBindingRepository = notebookGitBindingRepository;
  }

  public NotebookGitBinding createBindingForNotebook(Notebook notebook, Instant cutoverTime) {
    BundleWriteResult written = buildBundle(notebook, cutoverTime);

    NotebookGitBinding binding = new NotebookGitBinding();
    binding.setNotebook(notebook);
    applyBundle(binding, written, cutoverTime);
    return notebookGitBindingRepository.save(binding);
  }

  /**
   * Testability-only: replaces {@code notebook}'s existing accepted Git binding with a fresh
   * snapshot of its current content, as though cutover had just been re-run. Production now keeps
   * content bindings current for supported Portable content edits (SEED-009 Story 3), so this hook
   * remains only for fixture setup involving structural changes that production does not yet keep
   * in sync (folder moves, renames, and other Stories 4–7 cases). Never call this from a
   * production/user-facing path.
   */
  public NotebookGitBinding resnapshotForTestability(Notebook notebook, Instant snapshotTime) {
    BundleWriteResult written = buildBundle(notebook, snapshotTime);

    NotebookGitBinding binding =
        notebookGitBindingRepository
            .findByNotebook_Id(notebook.getId())
            .orElseGet(
                () -> {
                  NotebookGitBinding created = new NotebookGitBinding();
                  created.setNotebook(notebook);
                  return created;
                });
    applyBundle(binding, written, snapshotTime);
    return notebookGitBindingRepository.save(binding);
  }

  private BundleWriteResult buildBundle(Notebook notebook, Instant commitTime) {
    List<PortableTreeEntry> entries = buildPortableTreeEntries(notebook);
    try (Repository gitRepository =
        NotebookGitBundleBuilder.build(
            entries, SYSTEM_AUTHOR_NAME, SYSTEM_AUTHOR_EMAIL, CUTOVER_COMMIT_MESSAGE, commitTime)) {
      return NotebookGitBundleWriter.write(gitRepository);
    }
  }

  private void applyBundle(NotebookGitBinding binding, BundleWriteResult written, Instant time) {
    binding.setAcceptedGitObjectId(written.headObjectId());
    binding.setBundleBytes(written.bundleBytes());
    Timestamp timestamp = Timestamp.from(time);
    if (binding.getCreatedAt() == null) {
      binding.setCreatedAt(timestamp);
    }
    binding.setUpdatedAt(timestamp);
  }

  private List<PortableTreeEntry> buildPortableTreeEntries(Notebook notebook) {
    List<ExportFolderRow> folders = NotebookExportRows.folders(folderRepository, notebook);
    List<ExportNoteRow> notes = NotebookExportRows.notes(noteRepository, notebook);
    return PortableTreeSnapshot.build(notebook.getReadmeContent(), folders, notes);
  }
}
