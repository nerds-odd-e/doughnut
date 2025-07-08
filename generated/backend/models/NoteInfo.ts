/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { MemoryTracker } from './MemoryTracker';
import type { NoteRealm } from './NoteRealm';
import type { RecallSetting } from './RecallSetting';
export type NoteInfo = {
    memoryTrackers?: Array<MemoryTracker>;
    note: NoteRealm;
    createdAt: string;
    recallSetting?: RecallSetting;
};

