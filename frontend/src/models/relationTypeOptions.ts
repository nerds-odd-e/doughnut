import { parseNoteContentMarkdown } from "@/utils/noteContentFrontmatter"

export type RelationTypeLabel =
  | "related to"
  | "a specialization of"
  | "an application of"
  | "an instance of"
  | "a part of"
  | "tagged by"
  | "an attribute of"
  | "the opposite of"
  | "author of"
  | "using"
  | "an example of"
  | "before"
  | "similar to"
  | "confused with"

interface RelationTypeOption {
  label: RelationTypeLabel
  reversedLabel: string
}

const relationTypeOptions: RelationTypeOption[] = [
  {
    reversedLabel: "related to",
    label: "related to",
  },
  {
    reversedLabel: "a generalization of",
    label: "a specialization of",
  },
  {
    reversedLabel: "applied to",
    label: "an application of",
  },
  {
    reversedLabel: "has instances",
    label: "an instance of",
  },
  {
    reversedLabel: "has parts",
    label: "a part of",
  },
  {
    reversedLabel: "tagging",
    label: "tagged by",
  },
  {
    reversedLabel: "has attributes",
    label: "an attribute of",
  },
  {
    reversedLabel: "the opposite of",
    label: "the opposite of",
  },
  {
    reversedLabel: "brought by",
    label: "author of",
  },
  {
    reversedLabel: "used by",
    label: "using",
  },
  {
    reversedLabel: "has examples",
    label: "an example of",
  },
  {
    reversedLabel: "after",
    label: "before",
  },
  {
    reversedLabel: "similar to",
    label: "similar to",
  },
  {
    reversedLabel: "confused with",
    label: "confused with",
  },
]

const reverseLabel = (lbl: RelationTypeLabel | undefined) => {
  if (!lbl) return
  const relationType = relationTypeOptions.find(({ label }) => lbl === label)
  if (relationType) return relationType.reversedLabel
  return "*unknown relation type*"
}

/** Same kebab rule as `relationKebabFromLabel` below. */
export function relationKebabFromLabel(label: string): string {
  const t = label.trim()
  if (!t) return relationKebabFromLabel(relationTypeOptions[0]!.label)
  return t.toLowerCase().replace(/\s+/g, "-")
}

export function relationLabelFromKebab(kebab: string): string {
  const k = kebab.trim().toLowerCase()
  const found = relationTypeOptions.find(
    ({ label }) => relationKebabFromLabel(label) === k
  )
  if (found) return found.label
  return kebab.trim()
}

export function isKnownRelationKebab(kebab: string): boolean {
  const k = kebab.trim().toLowerCase()
  return relationTypeOptions.some(
    ({ label }) => relationKebabFromLabel(label) === k
  )
}

export function relationTypeFromKebab(kebab: string): RelationTypeLabel {
  const label = relationLabelFromKebab(kebab)
  const found = relationTypeOptions.find(({ label: l }) => l === label)
  return found?.label ?? relationTypeOptions[0]!.label
}

function relationKebabFromProperties(
  properties: Record<string, string>
): string | undefined {
  for (const key of Object.keys(properties)) {
    if (key.trim().toLowerCase() === "relation") {
      const v = properties[key]?.trim()
      if (!v) return
      return v
    }
  }
  return
}

/** Relation type label for display from note Markdown `relation` frontmatter (same mapping as rich property editor). */
export function relationTypeLabelFromNoteContent(
  markdown: string | undefined | null
): RelationTypeLabel | undefined {
  if (markdown == null) return
  const parsed = parseNoteContentMarkdown(markdown)
  if (!parsed.ok) return
  const kebab = relationKebabFromProperties(parsed.properties)
  if (kebab === undefined) return
  return relationTypeFromKebab(kebab)
}

export { relationTypeOptions, reverseLabel }
