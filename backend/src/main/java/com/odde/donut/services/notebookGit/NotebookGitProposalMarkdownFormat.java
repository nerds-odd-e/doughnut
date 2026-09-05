package com.odde.donut.services.notebookGit;

import com.odde.donut.algorithms.Frontmatter;
import com.odde.donut.algorithms.NoteLeadingFrontmatter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

/**
 * Walks every Markdown path in a proposal's proposed tree and requires the strict typed-Markdown
 * contract: strictly valid UTF-8 bytes, and - when a leading {@code ---} fence is present - YAML
 * that parses to a mapping carrying a non-blank scalar {@code type}. Unlike {@link
 * com.odde.donut.algorithms.NoteLeadingFrontmatter#ensureTypeKey}, this never repairs content; it
 * only accepts or rejects the bytes exactly as authored. An author-chosen {@code type} value that
 * is not one of Donut's recognized canonical types is still valid here - only structural problems
 * (missing fence, malformed YAML, non-mapping top level, or a missing/blank/non-scalar {@code
 * type}) are rejected.
 */
public final class NotebookGitProposalMarkdownFormat {

  private NotebookGitProposalMarkdownFormat() {}

  /**
   * @throws ResponseStatusException 400 BAD_REQUEST naming the offending path and the specific
   *     problem when any {@code .md} blob in the proposed tree fails the strict typed-Markdown
   *     contract, or when the tree cannot be inspected
   */
  public static void assertValidTypedMarkdown(Repository repository, ObjectId proposedHead) {
    try (RevWalk revWalk = new RevWalk(repository)) {
      RevCommit proposedCommit = revWalk.parseCommit(proposedHead);
      walkMarkdownFiles(repository, proposedCommit.getTree());
    } catch (IOException e) {
      throw invalidMarkdown("proposal tree could not be inspected", e);
    }
  }

  private static void walkMarkdownFiles(Repository repository, RevTree tree) throws IOException {
    try (TreeWalk walk = new TreeWalk(repository)) {
      walk.addTree(tree);
      walk.setRecursive(true);
      while (walk.next()) {
        String path = walk.getPathString();
        if (!path.endsWith(".md")) {
          continue;
        }
        ObjectLoader loader = repository.open(walk.getObjectId(0));
        assertValidTypedMarkdown(path, loader.getBytes());
      }
    }
  }

  private static void assertValidTypedMarkdown(String path, byte[] bytes) {
    String content = strictlyDecodeUtf8(path, bytes);
    Optional<NoteLeadingFrontmatter.VerbatimSplit> verbatim =
        NoteLeadingFrontmatter.splitVerbatim(content);
    if (verbatim.isEmpty()) {
      throw invalidMarkdown(
          "path \"" + path + "\" is missing frontmatter: no leading \"---\" fenced block");
    }

    String yamlRaw = verbatim.get().yamlRaw();
    Object loaded;
    try {
      loaded = new Yaml().load(yamlRaw);
    } catch (YAMLException e) {
      throw invalidMarkdown("path \"" + path + "\" has malformed frontmatter YAML", e);
    }

    if (!(loaded instanceof Map)) {
      throw invalidMarkdown(
          "path \"" + path + "\" frontmatter is not a mapping (top-level YAML must be a map)");
    }

    Optional<String> type = Frontmatter.parse(yamlRaw).getString("type").filter(s -> !s.isBlank());
    if (type.isEmpty()) {
      throw invalidMarkdown("path \"" + path + "\" frontmatter is missing a non-blank \"type\"");
    }
  }

  private static String strictlyDecodeUtf8(String path, byte[] bytes) {
    CharsetDecoder decoder =
        StandardCharsets.UTF_8
            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT);
    try {
      return decoder.decode(ByteBuffer.wrap(bytes)).toString();
    } catch (CharacterCodingException e) {
      throw invalidMarkdown("path \"" + path + "\" is not valid UTF-8", e);
    }
  }

  private static ResponseStatusException invalidMarkdown(String reason) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Markdown: " + reason);
  }

  private static ResponseStatusException invalidMarkdown(String reason, Throwable cause) {
    return new ResponseStatusException(
        HttpStatus.BAD_REQUEST, "Invalid Markdown: " + reason, cause);
  }
}
