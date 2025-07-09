/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type FocusNote = {
    uri?: string;
    subjectUriAndTitle?: string;
    predicate?: string;
    title?: string;
    objectUriAndTitle?: string;
    parentUriAndTitle?: string;
    relationToFocusNote?: FocusNote.relationToFocusNote;
    details?: string;
    contextualPath?: Array<string>;
    children?: Array<string>;
    priorSiblings?: Array<string>;
    youngerSiblings?: Array<string>;
    inboundReferences?: Array<string>;
};
export namespace FocusNote {
    export enum relationToFocusNote {
        SELF = 'Self',
        PARENT = 'Parent',
        OBJECT = 'Object',
        CHILD = 'Child',
        PRIOR_SIBLING = 'PriorSibling',
        YOUNGER_SIBLING = 'YoungerSibling',
        INBOUND_REFERENCE = 'InboundReference',
        SUBJECT_OF_INBOUND_REFERENCE = 'SubjectOfInboundReference',
        ANCESTOR_IN_CONTEXTUAL_PATH = 'AncestorInContextualPath',
        ANCESTOR_IN_OBJECT_CONTEXTUAL_PATH = 'AncestorInObjectContextualPath',
        OBJECT_OF_REIFIED_CHILD = 'ObjectOfReifiedChild',
        SIBLING_OF_PARENT = 'SiblingOfParent',
        SIBLING_OF_PARENT_OF_OBJECT = 'SiblingOfParentOfObject',
        CHILD_OF_SIBLING_OF_PARENT = 'ChildOfSiblingOfParent',
        CHILD_OF_SIBLING_OF_PARENT_OF_OBJECT = 'ChildOfSiblingOfParentOfObject',
        INBOUND_REFERENCE_CONTEXTUAL_PATH = 'InboundReferenceContextualPath',
        SIBLING_OF_SUBJECT_OF_INBOUND_REFERENCE = 'SiblingOfSubjectOfInboundReference',
        INBOUND_REFERENCE_TO_OBJECT_OF_REIFIED_CHILD = 'InboundReferenceToObjectOfReifiedChild',
    }
}

