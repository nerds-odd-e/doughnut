import type { NoteTopology } from "@generated/backend"
// Using string literals for relationType values

interface RelationTypeOption {
  label: NoteTopology["relationType"]
  reversedLabel: string
}

const relationTypeOptions = [
  {
    reversedLabel: "related to",
    label: "related to" as NoteTopology["relationType"],
  },
  {
    reversedLabel: "a generalization of",
    label: "a specialization of" as NoteTopology["relationType"],
  },
  {
    reversedLabel: "applied to",
    label: "an application of" as NoteTopology["relationType"],
  },
  {
    reversedLabel: "has instances",
    label: "an instance of" as NoteTopology["relationType"],
  },
  {
    reversedLabel: "has parts",
    label: "a part of" as NoteTopology["relationType"],
  },
  {
    reversedLabel: "tagging",
    label: "tagged by" as NoteTopology["relationType"],
  },
  {
    reversedLabel: "has attributes",
    label: "an attribute of" as NoteTopology["relationType"],
  },
  {
    reversedLabel: "the opposite of",
    label: "the opposite of" as NoteTopology["relationType"],
  },
  {
    reversedLabel: "brought by",
    label: "author of" as NoteTopology["relationType"],
  },
  {
    reversedLabel: "used by",
    label: "using" as NoteTopology["relationType"],
  },
  {
    reversedLabel: "has examples",
    label: "an example of" as NoteTopology["relationType"],
  },
  {
    reversedLabel: "after",
    label: "before" as NoteTopology["relationType"],
  },
  {
    reversedLabel: "similar to",
    label: "similar to" as NoteTopology["relationType"],
  },
  {
    reversedLabel: "confused with",
    label: "confused with" as NoteTopology["relationType"],
  },
] as RelationTypeOption[]

const reverseLabel = (lbl: NoteTopology["relationType"] | undefined) => {
  if (!lbl) return undefined
  const relationType = relationTypeOptions.find(({ label }) => lbl === label)
  if (relationType) return relationType.reversedLabel
  return "*unknown relation type*"
}

export { relationTypeOptions, reverseLabel }
