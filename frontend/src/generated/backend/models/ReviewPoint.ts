/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Note } from './Note';
export type ReviewPoint = {
    id: number;
    note: Note;
    lastReviewedAt?: string;
    nextReviewAt: string;
    initialReviewedAt?: string;
    repetitionCount?: number;
    forgettingCurveIndex?: number;
    removedFromReview?: boolean;
};

