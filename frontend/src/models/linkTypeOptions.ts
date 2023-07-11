interface LinkTypeOption {
  label: Generated.LinkType;
  reversedLabel: string;
}

const linkTypeOptions = [
  {
    reversedLabel: "no link",
    label: "no link",
  },
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
] as LinkTypeOption[];

const taggingTypes = linkTypeOptions
  .filter((t) => t.label === "tagged by")
  .map((t) => t.label);

const groupedTypes = linkTypeOptions
  .filter((t) =>
    ["related to", "the opposite of", "similar to", "confused with"].includes(
      t.label,
    ),
  )
  .map((t) => t.label);

const reverseLabel = (lbl) => {
  const linkType = linkTypeOptions.find(({ label }) => lbl === label);
  if (linkType) return linkType.reversedLabel;
  return "*unknown link type*";
};

export { linkTypeOptions, taggingTypes, groupedTypes, reverseLabel };
