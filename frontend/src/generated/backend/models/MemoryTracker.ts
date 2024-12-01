/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Note } from './Note';
export type MemoryTracker = {
    id: number;
    note: Note;
    lastRecalledAt?: string;
    nextRecallAt: string;
    onboardedAt?: string;
    repetitionCount?: number;
    forgettingCurveIndex?: number;
    removedFromTracking?: boolean;
};

