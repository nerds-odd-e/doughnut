/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Circle } from './Circle';
import type { LinkViewed } from './LinkViewed';
import type { Note } from './Note';
export type NoteRealm = {
    links?: Record<string, LinkViewed>;
    note: Note;
    fromBazaar?: boolean;
    circle?: Circle;
    id: number;
    children?: Array<Note>;
};

