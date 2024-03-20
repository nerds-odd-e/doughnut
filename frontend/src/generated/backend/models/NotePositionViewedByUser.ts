/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Circle } from './Circle';
import type { Note } from './Note';
export type NotePositionViewedByUser = {
    noteId?: number;
    fromBazaar?: boolean;
    circle?: Circle;
    ancestors?: Array<Note>;
};

