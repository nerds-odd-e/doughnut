/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Note } from './Note';
import type { NotebookSettings } from './NotebookSettings';
export type Notebook = {
    id: number;
    headNote: Note;
    certifiable?: boolean;
    notebookSettings: NotebookSettings;
    creatorId?: string;
    updated_at: string;
};

