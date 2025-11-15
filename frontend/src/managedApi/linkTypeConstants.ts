// Constants for linkType values
// This provides the namespace-like access that the frontend code expects

export const NoteTopology = {
  linkType: {
    NO_LINK: "no link" as const,
    RELATED_TO: "related to" as const,
    A_SPECIALIZATION_OF: "a specialization of" as const,
    AN_APPLICATION_OF: "an application of" as const,
    AN_INSTANCE_OF: "an instance of" as const,
    A_PART_OF: "a part of" as const,
    TAGGED_BY: "tagged by" as const,
    AN_ATTRIBUTE_OF: "an attribute of" as const,
    THE_OPPOSITE_OF: "the opposite of" as const,
    AUTHOR_OF: "author of" as const,
    USING: "using" as const,
    AN_EXAMPLE_OF: "an example of" as const,
    BEFORE: "before" as const,
    SIMILAR_TO: "similar to" as const,
    CONFUSED_WITH: "confused with" as const,
  },
}
