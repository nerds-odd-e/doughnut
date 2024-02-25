/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Note } from './Note';
import type { Notebook } from './Notebook';
import type { User } from './User';
export type Subscription = {
    headNote?: Note;
    title?: string;
    id: number;
    dailyTargetOfNewNotes?: number;
    user?: User;
    notebook?: Notebook;
};

