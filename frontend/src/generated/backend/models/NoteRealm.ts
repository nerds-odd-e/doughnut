/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Note } from './Note';
import type { Notebook } from './Notebook';
export type NoteRealm = {
    id: number;
    note: Note;
    fromBazaar?: boolean;
    children?: Array<Note>;
    inboundReferences?: Array<Note>;
    notebook?: Notebook;
};

