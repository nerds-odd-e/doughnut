import type { NoteTopology } from "@generated/backend"
// Using string literals for linkType values

interface LinkTypeOption {
  label: NoteTopology["linkType"]
  reversedLabel: string
}

const linkTypeOptions = [
  {
    reversedLabel: "no link",
    label: "no link" as NoteTopology["linkType"],
  },
  {
    reversedLabel: "related to",
    label: "related to" as NoteTopology["linkType"],
  },
  {
    reversedLabel: "a generalization of",
    label: "a specialization of" as NoteTopology["linkType"],
  },
  {
    reversedLabel: "applied to",
    label: "an application of" as NoteTopology["linkType"],
  },
  {
    reversedLabel: "has instances",
    label: "an instance of" as NoteTopology["linkType"],
  },
  {
    reversedLabel: "has parts",
    label: "a part of" as NoteTopology["linkType"],
  },
  {
    reversedLabel: "tagging",
    label: "tagged by" as NoteTopology["linkType"],
  },
  {
    reversedLabel: "has attributes",
    label: "an attribute of" as NoteTopology["linkType"],
  },
  {
    reversedLabel: "the opposite of",
    label: "the opposite of" as NoteTopology["linkType"],
  },
  {
    reversedLabel: "brought by",
    label: "author of" as NoteTopology["linkType"],
  },
  {
    reversedLabel: "used by",
    label: "using" as NoteTopology["linkType"],
  },
  {
    reversedLabel: "has examples",
    label: "an example of" as NoteTopology["linkType"],
  },
  {
    reversedLabel: "after",
    label: "before" as NoteTopology["linkType"],
  },
  {
    reversedLabel: "similar to",
    label: "similar to" as NoteTopology["linkType"],
  },
  {
    reversedLabel: "confused with",
    label: "confused with" as NoteTopology["linkType"],
  },
] as LinkTypeOption[]

const reverseLabel = (lbl) => {
  const linkType = linkTypeOptions.find(({ label }) => lbl === label)
  if (linkType) return linkType.reversedLabel
  return "*unknown link type*"
}

export { linkTypeOptions, reverseLabel }
