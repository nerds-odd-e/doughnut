/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { MemoryStatus } from './MemoryStatus';
import type { NoteLink } from './NoteLink';
export type NoteExport = {
    id?: number;
    title?: string;
    details?: string;
    wikidataId?: string;
    createdAt?: string;
    updatedAt?: string;
    links?: Array<NoteLink>;
    memoryStatus?: MemoryStatus;
};

