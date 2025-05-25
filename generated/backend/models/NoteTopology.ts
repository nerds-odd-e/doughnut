/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type NoteTopology = {
    id: number;
    titleOrPredicate: string;
    shortDetails?: string;
    linkType?: NoteTopology.linkType;
    objectNoteTopology?: NoteTopology;
    parentOrSubjectNoteTopology?: NoteTopology;
};
export namespace NoteTopology {
    export enum linkType {
        NO_LINK = 'no link',
        RELATED_TO = 'related to',
        A_SPECIALIZATION_OF = 'a specialization of',
        AN_APPLICATION_OF = 'an application of',
        AN_INSTANCE_OF = 'an instance of',
        A_PART_OF = 'a part of',
        TAGGED_BY = 'tagged by',
        AN_ATTRIBUTE_OF = 'an attribute of',
        THE_OPPOSITE_OF = 'the opposite of',
        AUTHOR_OF = 'author of',
        USING = 'using',
        AN_EXAMPLE_OF = 'an example of',
        BEFORE = 'before',
        SIMILAR_TO = 'similar to',
        CONFUSED_WITH = 'confused with',
    }
}

