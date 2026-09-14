package com.odde.donut.services.notebookGit;

import com.odde.donut.services.notebookGit.NotebookGitProposalTreeShape.InspectedRegularFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;

/**
 * Recognizes one exact same-name folder relocation from removed and added README paths using
 * complete relative-path/blob correspondence. Adjacent first-parent steps carry that exact mapping
 * across a range so a later descendant edit still resolves to the accepted source Folder. Inexact
 * README relocations are refused; residuals outside the relocated prefixes, plus tip concept edits
 * or additions under the destination after a carried relocate, feed ordinary note admission.
 */
final class NotebookGitProposalFolderShape {

  private NotebookGitProposalFolderShape() {}

  record FolderRelocation(String sourcePrefix, String destPrefix) {}

  /**
   * Accepted→tip mapping carried through adjacent exact relocations when present; otherwise the
   * tip-exact subtree mapping. Empty when neither an adjacent exact relocation nor tip README
   * remove/add establishes a unique exact same-name correspondence. Prefer carried first so tip
   * blob drift after an exact step does not refuse the range.
   */
  static Optional<FolderRelocation> requireExactOrCarried(
      Repository repository,
      ObjectId acceptedHead,
      ObjectId proposedHead,
      List<InspectedRegularFile> tipFiles) {
    Optional<FolderRelocation> carried =
        carryExactFolderRelocation(repository, acceptedHead, proposedHead);
    if (carried.isPresent()) {
      return carried;
    }
    return requireExactOrEmpty(tipFiles);
  }

  /**
   * @return the unique exact same-name subtree mapping when complete relative-path/blob
   *     correspondence holds; empty when no README is both removed and added. Outside residual
   *     changes are left for the publisher's ordinary note application.
   * @throws org.springframework.web.server.ResponseStatusException when README paths moved but the
   *     correspondence is not a unique exact subtree relocation
   */
  static Optional<FolderRelocation> requireExactOrEmpty(List<InspectedRegularFile> files) {
    List<String> removedReadmes = new ArrayList<>();
    List<String> addedReadmes = new ArrayList<>();
    for (InspectedRegularFile file : files) {
      if (!isReadme(file.path())) {
        continue;
      }
      if (file.acceptedBlobId() != null && file.proposedBlobId() == null) {
        removedReadmes.add(file.path());
      } else if (file.acceptedBlobId() == null && file.proposedBlobId() != null) {
        addedReadmes.add(file.path());
      }
    }
    if (removedReadmes.isEmpty() || addedReadmes.isEmpty()) {
      return Optional.empty();
    }

    List<String> sourcePrefixes = outermost(prefixesOf(removedReadmes));
    List<String> destPrefixes = outermost(prefixesOf(addedReadmes));
    List<FolderRelocation> complete = new ArrayList<>();
    String incompletePath = null;
    for (String source : sourcePrefixes) {
      for (String dest : destPrefixes) {
        if (!folderName(source).equals(folderName(dest))) {
          continue;
        }
        String breaker = correspondenceBreaker(files, source, dest);
        if (breaker == null) {
          complete.add(new FolderRelocation(source, dest));
        } else if (incompletePath == null) {
          incompletePath = breaker;
        }
      }
    }

    if (complete.size() > 1) {
      throw NotebookGitProposalTreeShape.unsupportedTreeShape(
          inexactReason(complete.get(1).sourcePrefix() + "/README.md"));
    }
    if (complete.size() == 1) {
      return Optional.of(complete.get(0));
    }
    if (incompletePath != null) {
      throw NotebookGitProposalTreeShape.unsupportedTreeShape(inexactReason(incompletePath));
    }
    throw NotebookGitProposalTreeShape.unsupportedTreeShape(inexactReason(addedReadmes.get(0)));
  }

  /**
   * Composes adjacent exact same-name folder relocations into one accepted→tip prefix mapping.
   * Incomplete adjacent correspondence still refuses; tip blob drift after an exact step does not.
   */
  static Optional<FolderRelocation> carryExactFolderRelocation(
      Repository repository, ObjectId acceptedHead, ObjectId proposedHead) {
    List<ObjectId> range =
        NotebookGitProposalAncestry.firstParentRange(repository, acceptedHead, proposedHead);
    Map<String, String> originByCurrent = new HashMap<>();
    for (int i = 1; i < range.size(); i++) {
      List<InspectedRegularFile> stepFiles =
          NotebookGitProposalTreeShape.inspectRegularFiles(
              repository, range.get(i - 1), range.get(i));
      Optional<FolderRelocation> step = requireExactOrEmpty(stepFiles);
      if (step.isEmpty()) {
        continue;
      }
      FolderRelocation relocation = step.get();
      String acceptedOrigin =
          originByCurrent.getOrDefault(relocation.sourcePrefix(), relocation.sourcePrefix());
      originByCurrent.remove(relocation.sourcePrefix());
      originByCurrent.put(relocation.destPrefix(), acceptedOrigin);
    }
    FolderRelocation composed = null;
    for (Map.Entry<String, String> entry : originByCurrent.entrySet()) {
      if (entry.getKey().equals(entry.getValue())) {
        continue;
      }
      if (composed != null) {
        throw NotebookGitProposalTreeShape.unsupportedTreeShape(
            inexactReason(entry.getValue() + "/README.md"));
      }
      composed = new FolderRelocation(entry.getValue(), entry.getKey());
    }
    return Optional.ofNullable(composed);
  }

  /**
   * Changed files outside the exact relocated source and destination prefixes, plus tip concept
   * edits or additions under the destination that are not equal-blob copies of the accepted source
   * subtree. Unchanged relocated content is omitted so the publisher can reclassify residuals only.
   */
  static List<InspectedRegularFile> residualOutside(
      List<InspectedRegularFile> files, FolderRelocation relocation) {
    Map<String, ObjectId> acceptedUnderSource = new HashMap<>();
    for (InspectedRegularFile file : files) {
      if (file.acceptedBlobId() != null && under(file.path(), relocation.sourcePrefix())) {
        acceptedUnderSource.put(
            relative(file.path(), relocation.sourcePrefix()), file.acceptedBlobId());
      }
    }
    List<InspectedRegularFile> residual = new ArrayList<>();
    for (InspectedRegularFile file : files) {
      if (under(file.path(), relocation.sourcePrefix()) || sameBlob(file)) {
        continue;
      }
      if (under(file.path(), relocation.destPrefix())) {
        InspectedRegularFile underDest =
            residualUnderDestination(file, relocation, acceptedUnderSource);
        if (underDest != null) {
          residual.add(underDest);
        }
        continue;
      }
      residual.add(file);
    }
    return residual;
  }

  private static InspectedRegularFile residualUnderDestination(
      InspectedRegularFile file,
      FolderRelocation relocation,
      Map<String, ObjectId> acceptedUnderSource) {
    if (file.proposedBlobId() == null || isReadme(file.path())) {
      return null;
    }
    String relative = relative(file.path(), relocation.destPrefix());
    ObjectId acceptedBlob = acceptedUnderSource.get(relative);
    if (acceptedBlob == null) {
      return file;
    }
    if (acceptedBlob.equals(file.proposedBlobId())) {
      return null;
    }
    return new InspectedRegularFile(file.path(), acceptedBlob, file.proposedBlobId());
  }

  private static boolean sameBlob(InspectedRegularFile file) {
    return file.acceptedBlobId() != null
        && file.proposedBlobId() != null
        && file.acceptedBlobId().equals(file.proposedBlobId());
  }

  private static String inexactReason(String path) {
    return "path \"" + path + "\" is not an exact folder relocation";
  }

  private static String correspondenceBreaker(
      List<InspectedRegularFile> files, String sourcePrefix, String destPrefix) {
    Map<String, ObjectId> acceptedRelative = new LinkedHashMap<>();
    Map<String, ObjectId> proposedRelative = new LinkedHashMap<>();
    String leftover = null;
    for (InspectedRegularFile file : files) {
      if (file.acceptedBlobId() != null && under(file.path(), sourcePrefix)) {
        acceptedRelative.put(relative(file.path(), sourcePrefix), file.acceptedBlobId());
        if (file.proposedBlobId() != null && leftover == null) {
          leftover = file.path();
        }
      }
      if (file.proposedBlobId() != null && under(file.path(), destPrefix)) {
        proposedRelative.put(relative(file.path(), destPrefix), file.proposedBlobId());
      }
    }
    if (leftover != null) {
      return leftover;
    }
    List<String> relatives = new ArrayList<>(acceptedRelative.keySet());
    for (String rel : proposedRelative.keySet()) {
      if (!acceptedRelative.containsKey(rel)) {
        relatives.add(rel);
      }
    }
    for (String rel : relatives) {
      ObjectId acceptedBlob = acceptedRelative.get(rel);
      ObjectId proposedBlob = proposedRelative.get(rel);
      if (acceptedBlob == null || proposedBlob == null || !acceptedBlob.equals(proposedBlob)) {
        return join(destPrefix, rel);
      }
    }
    return null;
  }

  private static List<String> prefixesOf(List<String> readmePaths) {
    List<String> prefixes = new ArrayList<>(readmePaths.size());
    for (String path : readmePaths) {
      prefixes.add(prefix(path));
    }
    return prefixes;
  }

  private static List<String> outermost(List<String> prefixes) {
    List<String> result = new ArrayList<>();
    for (String prefix : prefixes) {
      boolean nested = false;
      for (String other : prefixes) {
        if (!other.equals(prefix) && under(prefix, other)) {
          nested = true;
          break;
        }
      }
      if (!nested && !result.contains(prefix)) {
        result.add(prefix);
      }
    }
    return result;
  }

  private static boolean isReadme(String path) {
    int lastSlash = path.lastIndexOf('/');
    String basename = lastSlash < 0 ? path : path.substring(lastSlash + 1);
    return "README.md".equals(basename);
  }

  private static String prefix(String readmePath) {
    int lastSlash = readmePath.lastIndexOf('/');
    return lastSlash < 0 ? "" : readmePath.substring(0, lastSlash);
  }

  private static String folderName(String prefix) {
    int lastSlash = prefix.lastIndexOf('/');
    return lastSlash < 0 ? prefix : prefix.substring(lastSlash + 1);
  }

  private static boolean under(String path, String prefix) {
    if (prefix.isEmpty()) {
      return true;
    }
    return path.startsWith(prefix + "/");
  }

  private static String relative(String path, String prefix) {
    return prefix.isEmpty() ? path : path.substring(prefix.length() + 1);
  }

  private static String join(String prefix, String relative) {
    return prefix.isEmpty() ? relative : prefix + "/" + relative;
  }
}
