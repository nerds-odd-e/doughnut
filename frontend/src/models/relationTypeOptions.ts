import type { NoteTopology } from "@generated/doughnut-backend-api"

type RelationTypeLabel = NonNullable<NoteTopology["relationType"]>

interface RelationTypeOption {
  label: RelationTypeLabel
  reversedLabel: string
}

const relationTypeOptions = [
  {
    reversedLabel: "related to",
    label: "related to" as RelationTypeLabel,
  },
  {
    reversedLabel: "a generalization of",
    label: "a specialization of" as RelationTypeLabel,
  },
  {
    reversedLabel: "applied to",
    label: "an application of" as RelationTypeLabel,
  },
  {
    reversedLabel: "has instances",
    label: "an instance of" as RelationTypeLabel,
  },
  {
    reversedLabel: "has parts",
    label: "a part of" as RelationTypeLabel,
  },
  {
    reversedLabel: "tagging",
    label: "tagged by" as RelationTypeLabel,
  },
  {
    reversedLabel: "has attributes",
    label: "an attribute of" as RelationTypeLabel,
  },
  {
    reversedLabel: "the opposite of",
    label: "the opposite of" as RelationTypeLabel,
  },
  {
    reversedLabel: "brought by",
    label: "author of" as RelationTypeLabel,
  },
  {
    reversedLabel: "used by",
    label: "using" as RelationTypeLabel,
  },
  {
    reversedLabel: "has examples",
    label: "an example of" as RelationTypeLabel,
  },
  {
    reversedLabel: "after",
    label: "before" as RelationTypeLabel,
  },
  {
    reversedLabel: "similar to",
    label: "similar to" as RelationTypeLabel,
  },
  {
    reversedLabel: "confused with",
    label: "confused with" as RelationTypeLabel,
  },
] as RelationTypeOption[]

const reverseLabel = (lbl: RelationTypeLabel | undefined) => {
  if (!lbl) return
  const relationType = relationTypeOptions.find(({ label }) => lbl === label)
  if (relationType) return relationType.reversedLabel
  return "*unknown relation type*"
}

/** Same rule as backend RelationshipNoteMarkdownFormatter.labelToKebab */
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

export function relationTypeFromKebab(kebab: string): RelationTypeLabel {
  const label = relationLabelFromKebab(kebab)
  const found = relationTypeOptions.find(({ label: l }) => l === label)
  return found?.label ?? relationTypeOptions[0]!.label
}

export { relationTypeOptions, reverseLabel }
