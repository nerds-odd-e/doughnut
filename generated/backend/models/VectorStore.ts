/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { ExpiresAfter } from './ExpiresAfter';
import type { FileCounts } from './FileCounts';
export type VectorStore = {
    id?: string;
    object?: string;
    name?: string;
    bytes?: number;
    status?: string;
    metadata?: Record<string, string>;
    created_at?: number;
    file_counts?: FileCounts;
    expires_after?: ExpiresAfter;
    expires_at?: number;
    last_active_at?: number;
};

