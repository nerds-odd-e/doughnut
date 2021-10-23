const linkTypeOptions = [
    {
      "reversedLabel": "related to",
      "label": "related to",
      "value": 1
    },
    {
      "reversedLabel": "a generalization of",
      "label": "a specialization of",
      "value": 2
    },
    {
      "reversedLabel": "applied to",
      "label": "an application of",
      "value": 3
    },
    {
      "reversedLabel": "has instances",
      "label": "an instance of",
      "value": 4
    },
    {
      "reversedLabel": "has parts",
      "label": "a part of",
      "value": 6
    },
    {
      "reversedLabel": "tagging",
      "label": "tagged by",
      "value": 8
    },
    {
      "reversedLabel": "has attributes",
      "label": "an attribute of",
      "value": 10
    },
    {
      "reversedLabel": "the opposite of",
      "label": "the opposite of",
      "value": 12
    },
    {
      "reversedLabel": "brought by",
      "label": "author of",
      "value": 14
    },
    {
      "reversedLabel": "used by",
      "label": "using",
      "value": 15
    },
    {
      "reversedLabel": "has examples",
      "label": "an example of",
      "value": 17
    },
    {
      "reversedLabel": "after",
      "label": "before",
      "value": 19
    },
    {
      "reversedLabel": "similar to",
      "label": "similar to",
      "value": 22
    },
    {
      "reversedLabel": "confused with",
      "label": "confused with",
      "value": 23
    }
  ]

  const taggingTypes = linkTypeOptions
      .filter((t) => parseInt(t.value, 10) === 8)
      .map((t) => t.label);

  const groupedTypes = linkTypeOptions
      .filter((t) => [1, 12, 22, 23].includes(parseInt(t.value, 10)))
      .map((t) => t.label);

  const reverseLabel = (lbl) => {
    const linkType = linkTypeOptions.find(({ label }) => lbl === label);
    if (linkType) return linkType.reversedLabel;
    return "*unknown link type*";
  }

  const linkTypeNameToId = (name) => {
    const link = linkTypeOptions.find(v=>v.label===name)
    if (link) return link.value
    return 0
  }

  export { linkTypeOptions, taggingTypes, groupedTypes, reverseLabel, linkTypeNameToId }