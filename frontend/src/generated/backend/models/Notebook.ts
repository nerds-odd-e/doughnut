/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Note } from './Note';
import type { Ownership } from './Ownership';
export type Notebook = {
    id?: number;
    ownership?: Ownership;
    headNote?: Note;
    skipReviewEntirely?: boolean;
    deletedAt?: string;
};

