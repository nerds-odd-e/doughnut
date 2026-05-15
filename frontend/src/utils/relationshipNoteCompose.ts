import { relationKebabFromLabel } from "@/models/relationTypeOptions"
import type { RelationTypeLabel } from "@/models/relationTypeOptions"

const NOTE_TYPE = "relationship"
const UNTITLED = "Untitled"
const DEFAULT_RELATION_LABEL = "related to"
/** Matches backend `Note.MAX_TITLE_LENGTH`. */
const MAX_TITLE_LENGTH = 150

function trimmedOrEmpty(s: string | undefined | null): string {
  return s?.trim() ?? ""
}

function trimmedOrNull(s: string | undefined | null): string | null {
  if (s == null) return null
  const t = s.trim()
  return t === "" ? null : t
}

function displaySegment(trimmed: string, fallbackWhenBlank: string): string {
  return trimmed === "" ? fallbackWhenBlank : trimmed
}

function yamlDoubleQuotedInner(s: string): string {
  return s.replace(/\\/g, "\\\\").replace(/"/g, '\\"')
}

function wikiLink(display: string): string {
  return `[[${display}]]`
}

function displayTitle(title: string | undefined | null): string {
  const t = trimmedOrEmpty(title)
  return t === "" ? UNTITLED : t
}

function wikiTokenForEndpoint(
  relationshipNotebookId: number,
  endpoint: {
    title?: string | null
    notebookId: number
    notebookName?: string
  }
): string {
  const display = displayTitle(endpoint.title)
  if (endpoint.notebookId === relationshipNotebookId) {
    return wikiLink(display)
  }
  const nbName = trimmedOrEmpty(endpoint.notebookName)
  if (nbName === "") {
    return wikiLink(display)
  }
  return wikiLink(`${nbName}: ${display}`)
}

/** Composes the relationship note title (Untitled / max length aligned with `Note.MAX_TITLE_LENGTH`). */
export function formatRelationshipNoteTitle(
  sourceTitle: string | undefined | null,
  relationLabel: RelationTypeLabel | string | undefined | null,
  targetTitle: string | undefined | null
): string {
  const source = displaySegment(trimmedOrEmpty(sourceTitle), UNTITLED)
  const relation = displaySegment(
    trimmedOrEmpty(relationLabel),
    DEFAULT_RELATION_LABEL
  )
  const target = displaySegment(trimmedOrEmpty(targetTitle), UNTITLED)
  const composed = `${source} ${relation} ${target}`.trim()
  if (composed === "") {
    return UNTITLED
  }
  if (composed.length > MAX_TITLE_LENGTH) {
    return composed.slice(0, MAX_TITLE_LENGTH)
  }
  return composed
}

/** Composes relationship note markdown (YAML frontmatter only; no body line). */
export function formatRelationshipNoteMarkdown(args: {
  relationLabel: RelationTypeLabel | string
  sourceEndpoint: {
    title?: string | null
    notebookId: number
    notebookName?: string
  }
  targetEndpoint: {
    title?: string | null
    notebookId: number
    notebookName?: string
  }
  relationshipNotebookId: number
  preservedDetails?: string | null
}): string {
  const relationLabel =
    trimmedOrEmpty(args.relationLabel) === ""
      ? DEFAULT_RELATION_LABEL
      : args.relationLabel.trim()
  const relationKebab = relationKebabFromLabel(relationLabel)

  const sourceLink = wikiTokenForEndpoint(
    args.relationshipNotebookId,
    args.sourceEndpoint
  )
  const targetLink = wikiTokenForEndpoint(
    args.relationshipNotebookId,
    args.targetEndpoint
  )

  let out = "---\n"
  out += `type: ${NOTE_TYPE}\n`
  out += `relation: ${relationKebab}\n`
  out += `source: "${yamlDoubleQuotedInner(sourceLink)}"\n`
  out += `target: "${yamlDoubleQuotedInner(targetLink)}"\n`
  out += "---\n\n"

  const preserved = trimmedOrNull(args.preservedDetails)
  if (preserved != null) {
    out += `\n\n${preserved}`
  }
  return out
}
