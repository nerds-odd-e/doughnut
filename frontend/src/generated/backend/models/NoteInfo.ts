/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { NoteRealm } from './NoteRealm';
import type { ReviewPoint } from './ReviewPoint';
import type { ReviewSetting } from './ReviewSetting';
export type NoteInfo = {
    reviewPoint?: ReviewPoint;
    note: NoteRealm;
    createdAt: string;
    reviewSetting?: ReviewSetting;
};

