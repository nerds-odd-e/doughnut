package com.odde.donut.services.notebookGit;

import com.odde.donut.services.notebookGit.NotebookGitProposalTreeShape.InspectedRegularFile;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.eclipse.jgit.lib.ObjectId;

/**
 * Recognizes one exact same-name folder relocation from removed and added README paths using
 * complete relative-path/blob correspondence. Inexact README relocations are refused; an exact
 * candidate is returned so the publisher can reparent that source Folder.
 */
final class NotebookGitProposalFolderShape {

  private NotebookGitProposalFolderShape() {}

  record FolderRelocation(String sourcePrefix, String destPrefix) {}

  /**
   * @return the unique exact mapping when the proposal is one complete same-name README relocation
   *     with no other changes; empty when no README is both removed and added
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
      FolderRelocation mapping = complete.get(0);
      String other = firstChangeOutside(files, mapping.sourcePrefix(), mapping.destPrefix());
      if (other == null) {
        return Optional.of(mapping);
      }
      throw NotebookGitProposalTreeShape.unsupportedTreeShape(inexactReason(other));
    }
    if (incompletePath != null) {
      throw NotebookGitProposalTreeShape.unsupportedTreeShape(inexactReason(incompletePath));
    }
    throw NotebookGitProposalTreeShape.unsupportedTreeShape(inexactReason(addedReadmes.get(0)));
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

  private static String firstChangeOutside(
      List<InspectedRegularFile> files, String sourcePrefix, String destPrefix) {
    for (InspectedRegularFile file : files) {
      if (unchanged(file) || under(file.path(), sourcePrefix) || under(file.path(), destPrefix)) {
        continue;
      }
      return file.path();
    }
    return null;
  }

  private static boolean unchanged(InspectedRegularFile file) {
    return file.acceptedBlobId() != null
        && file.proposedBlobId() != null
        && file.acceptedBlobId().equals(file.proposedBlobId());
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
