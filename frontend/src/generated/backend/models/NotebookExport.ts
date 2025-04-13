/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { NotebookSettings } from './NotebookSettings';
import type { NoteExport } from './NoteExport';
export type NotebookExport = {
    id?: number;
    title?: string;
    shortDetails?: string;
    certifiable?: boolean;
    createdAt?: string;
    updatedAt?: string;
    settings?: NotebookSettings;
    notes?: Array<NoteExport>;
};

