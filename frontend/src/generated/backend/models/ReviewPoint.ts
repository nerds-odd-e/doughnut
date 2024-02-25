/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Thing } from './Thing';
export type ReviewPoint = {
    id?: number;
    lastReviewedAt?: string;
    nextReviewAt?: string;
    initialReviewedAt?: string;
    repetitionCount?: number;
    forgettingCurveIndex?: number;
    removedFromReview?: boolean;
    thing?: Thing;
};

