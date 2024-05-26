/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { LinkViewed } from './LinkViewed';
import type { Note } from './Note';
import type { NotePositionViewedByUser } from './NotePositionViewedByUser';
export type NoteRealm = {
    links?: Record<string, LinkViewed>;
    note: Note;
    notePosition: NotePositionViewedByUser;
    id: number;
    children?: Array<Note>;
};

