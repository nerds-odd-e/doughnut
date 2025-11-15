// Constants for NoteTopology linkType values
// This provides the namespace-like access that the test code expects

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
    CREATED_BY: "created by" as const,
    LOCATED_AT: "located at" as const,
    OCCURS_AT: "occurs at" as const,
    HAPPENED_AT: "happened at" as const,
    CONTAINS: "contains" as const,
  },
}
