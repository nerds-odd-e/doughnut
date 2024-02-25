/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Note } from './Note';
export type Thing = {
    note?: Note;
    linkType?: 'no link' | 'related to' | 'a specialization of' | 'an application of' | 'an instance of' | 'a part of' | 'tagged by' | 'an attribute of' | 'the opposite of' | 'author of' | 'using' | 'an example of' | 'before' | 'similar to' | 'confused with';
    sourceNote?: Note;
    targetNote?: Note;
    id?: number;
};

