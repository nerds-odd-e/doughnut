/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { MemoryTracker } from './MemoryTracker';
import type { NoteRealm } from './NoteRealm';
import type { ReviewSetting } from './ReviewSetting';
export type NoteInfo = {
    memoryTracker?: MemoryTracker;
    note: NoteRealm;
    createdAt: string;
    reviewSetting?: ReviewSetting;
};

