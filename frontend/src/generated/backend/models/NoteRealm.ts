/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Circle } from './Circle';
import type { Note } from './Note';
export type NoteRealm = {
    id: number;
    note: Note;
    fromBazaar?: boolean;
    circle?: Circle;
    children?: Array<Note>;
    refers?: Array<Note>;
};

