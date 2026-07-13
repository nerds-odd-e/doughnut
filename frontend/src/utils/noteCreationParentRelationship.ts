import {
  composeNoteContentMarkdown,
  frontmatterScalar,
  parseNoteContentMarkdown,
} from "@/utils/noteContentFrontmatter"
import { scalarPropertyValue } from "@/utils/noteProperties"

export type NoteCreationParentRelationship =
  | "none"
  | "under_current"
  | "same_parent"

export type ParentRelationshipOption = {
  value: NoteCreationParentRelationship
  label: string
}

/** Scalar `parent` frontmatter value from note markdown, if present. */
export function parentPropertyScalarFromMarkdown(
  markdown: string | undefined | null
): string | undefined {
  if (markdown == null || markdown === "") return
  const parsed = parseNoteContentMarkdown(markdown)
  if (!parsed.ok) return
  return frontmatterScalar(parsed.properties, "parent")
}

export function parentRelationshipOptions(
  contextContent?: string
): ParentRelationshipOption[] {
  const options: ParentRelationshipOption[] = [
    { value: "none", label: "None" },
    { value: "under_current", label: "Under current" },
  ]
  if (parentPropertyScalarFromMarkdown(contextContent) != null) {
    options.push({ value: "same_parent", label: "Same parent" })
  }
  return options
}

export function resolveCreateNoteParentValue(
  relationship: NoteCreationParentRelationship,
  contextNote: { title: string; content?: string }
): string | undefined {
  if (relationship === "none") return
  if (relationship === "under_current") {
    return `[[${contextNote.title}]]`
  }
  return parentPropertyScalarFromMarkdown(contextNote.content)
}

/** Merges a `parent` property into create-note content; leaves content unchanged when parent is unset. */
export function mergeParentIntoCreateContent(
  baseContent: string | undefined,
  parentValue: string | undefined
): string | undefined {
  if (parentValue === undefined) return baseContent

  const base = baseContent ?? ""
  const parsed = parseNoteContentMarkdown(base)
  if (!parsed.ok) {
    return composeNoteContentMarkdown({
      properties: { parent: scalarPropertyValue(parentValue) },
      body: base,
    })
  }

  return composeNoteContentMarkdown({
    properties: {
      ...parsed.properties,
      parent: scalarPropertyValue(parentValue),
    },
    body: parsed.body,
  })
}

export function applyParentRelationshipToCreateContent(
  baseContent: string | undefined,
  relationship: NoteCreationParentRelationship,
  contextNote: { title: string; content?: string } | undefined
): string | undefined {
  if (contextNote == null) return baseContent
  return mergeParentIntoCreateContent(
    baseContent,
    resolveCreateNoteParentValue(relationship, contextNote)
  )
}
