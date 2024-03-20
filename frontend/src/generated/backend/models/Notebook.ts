/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Note } from './Note';
import type { NotebookDTO } from './NotebookDTO';
import type { Ownership } from './Ownership';
export type Notebook = {
    id: number;
    ownership?: Ownership;
    headNote?: Note;
    skipReviewEntirely?: boolean;
    deletedAt?: string;
    fromDTO?: NotebookDTO;
};

