import type { NoteTopology } from "@generated/backend"
import { LinkCreation } from "@generated/backend"

interface LinkTypeOption {
  label: NoteTopology["linkType"]
  reversedLabel: string
}

const linkTypeOptions = [
  {
    reversedLabel: "no link",
    label: LinkCreation.linkType.NO_LINK,
  },
  {
    reversedLabel: "related to",
    label: LinkCreation.linkType.RELATED_TO,
  },
  {
    reversedLabel: "a generalization of",
    label: LinkCreation.linkType.A_SPECIALIZATION_OF,
  },
  {
    reversedLabel: "applied to",
    label: LinkCreation.linkType.AN_APPLICATION_OF,
  },
  {
    reversedLabel: "has instances",
    label: LinkCreation.linkType.AN_INSTANCE_OF,
  },
  {
    reversedLabel: "has parts",
    label: LinkCreation.linkType.A_PART_OF,
  },
  {
    reversedLabel: "tagging",
    label: LinkCreation.linkType.TAGGED_BY,
  },
  {
    reversedLabel: "has attributes",
    label: LinkCreation.linkType.AN_ATTRIBUTE_OF,
  },
  {
    reversedLabel: "the opposite of",
    label: LinkCreation.linkType.THE_OPPOSITE_OF,
  },
  {
    reversedLabel: "brought by",
    label: LinkCreation.linkType.AUTHOR_OF,
  },
  {
    reversedLabel: "used by",
    label: LinkCreation.linkType.USING,
  },
  {
    reversedLabel: "has examples",
    label: LinkCreation.linkType.AN_EXAMPLE_OF,
  },
  {
    reversedLabel: "after",
    label: LinkCreation.linkType.BEFORE,
  },
  {
    reversedLabel: "similar to",
    label: LinkCreation.linkType.SIMILAR_TO,
  },
  {
    reversedLabel: "confused with",
    label: LinkCreation.linkType.CONFUSED_WITH,
  },
] as LinkTypeOption[]

const reverseLabel = (lbl) => {
  const linkType = linkTypeOptions.find(({ label }) => lbl === label)
  if (linkType) return linkType.reversedLabel
  return "*unknown link type*"
}

export { linkTypeOptions, reverseLabel }
