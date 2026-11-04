package com.odde.donut.services.notebookGit;

import com.odde.donut.algorithms.Frontmatter;
import com.odde.donut.algorithms.NoteContentMarkdown;
import com.odde.donut.algorithms.NotePropertyIndexPlanner;
import com.odde.donut.algorithms.NotePropertyIndexPlanner.DatedValue;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.RenameDetector;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

/**
 * Dates the property values of a notebook's tip notes from a first-parent commit range: each value
 * takes the committer time of the newest commit in the range whose parsed value differs from its
 * parent's (a scalar by content, a list item by presence), following each note's path backward
 * through renames.
 */
public final class NotebookGitPropertyValueDating {

  /**
   * A tip note's value and when it last changed in the range; {@code committedAt} is empty when the
   * value is already in the range's parentless root commit.
   */
  public record ValueDate(
      String tipPath, String propertyKey, String propertyValue, Optional<Instant> committedAt) {}

  private record ValueKey(String propertyKey, String propertyValue) {}

  private record TrackedNote(String tipPath, Set<ValueKey> undated) {}

  private NotebookGitPropertyValueDating() {}

  /**
   * Values at {@code tip} changed in the commits after {@code olderExclusive} (or from the root
   * when null) through {@code tip}; values unchanged across the range have no entry.
   */
  public static List<ValueDate> dateValues(
      Repository repository, ObjectId olderExclusive, ObjectId tip) {
    try (RevWalk walk = new RevWalk(repository)) {
      RevCommit commit = walk.parseCommit(tip);
      Map<String, TrackedNote> trackedByPath = tipNotes(repository, commit);
      List<ValueDate> dates = new ArrayList<>();
      while (!commit.equals(olderExclusive) && !trackedByPath.isEmpty()) {
        if (commit.getParentCount() == 0) {
          trackedByPath
              .values()
              .forEach(note -> note.undated().forEach(v -> dates.add(dateOf(note, v, null))));
          break;
        }
        RevCommit parent = walk.parseCommit(commit.getParent(0));
        Instant committedAt = commit.getCommitterIdent().getWhenAsInstant();
        for (DiffEntry entry : changedFiles(repository, parent, commit)) {
          TrackedNote note = trackedByPath.remove(entry.getNewPath());
          if (note == null) {
            continue;
          }
          Frontmatter before =
              entry.getChangeType() == DiffEntry.ChangeType.ADD
                  ? Frontmatter.empty()
                  : frontmatterOf(repository, entry.getOldId().toObjectId());
          Frontmatter after = frontmatterOf(repository, entry.getNewId().toObjectId());
          for (DatedValue changed : NotePropertyIndexPlanner.changedValues(before, after)) {
            ValueKey key = new ValueKey(changed.propertyKey(), changed.propertyValue());
            if (note.undated().remove(key)) {
              dates.add(dateOf(note, key, committedAt));
            }
          }
          if (!note.undated().isEmpty()) {
            trackedByPath.put(entry.getOldPath(), note);
          }
        }
        commit = parent;
      }
      return dates;
    } catch (IOException e) {
      throw new UncheckedIOException("could not date property values from notebook history", e);
    }
  }

  private static ValueDate dateOf(TrackedNote note, ValueKey key, Instant committedAt) {
    return new ValueDate(
        note.tipPath(), key.propertyKey(), key.propertyValue(), Optional.ofNullable(committedAt));
  }

  private static Map<String, TrackedNote> tipNotes(Repository repository, RevCommit tip)
      throws IOException {
    Map<String, TrackedNote> notes = new HashMap<>();
    try (TreeWalk treeWalk = new TreeWalk(repository)) {
      treeWalk.setRecursive(true);
      treeWalk.addTree(tip.getTree());
      while (treeWalk.next()) {
        String path = treeWalk.getPathString();
        if (!PortablePathKind.isNote(path)) {
          continue;
        }
        Set<ValueKey> values =
            NotePropertyIndexPlanner.plannedRows(frontmatterOf(repository, treeWalk.getObjectId(0)))
                .stream()
                .map(row -> new ValueKey(row.propertyKey(), row.propertyValue()))
                .collect(Collectors.toCollection(HashSet::new));
        if (!values.isEmpty()) {
          notes.put(path, new TrackedNote(path, values));
        }
      }
    }
    return notes;
  }

  private static List<DiffEntry> changedFiles(
      Repository repository, RevCommit parent, RevCommit commit) throws IOException {
    try (TreeWalk treeWalk = new TreeWalk(repository)) {
      treeWalk.setRecursive(true);
      treeWalk.setFilter(TreeFilter.ANY_DIFF);
      treeWalk.addTree(parent.getTree());
      treeWalk.addTree(commit.getTree());
      RenameDetector detector = new RenameDetector(repository);
      detector.setRenameScore(NotebookGitProposalRenameDetector.RENAME_SCORE_THRESHOLD);
      detector.addAll(DiffEntry.scan(treeWalk));
      return detector.compute();
    }
  }

  private static Frontmatter frontmatterOf(Repository repository, AnyObjectId blobId)
      throws IOException {
    return NoteContentMarkdown.leadingFrontmatter(
        new String(repository.open(blobId).getBytes(), StandardCharsets.UTF_8));
  }
}
