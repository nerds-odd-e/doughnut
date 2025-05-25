/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { LastError } from './LastError';
import type { StepDetails } from './StepDetails';
import type { Usage } from './Usage';
export type RunStep = {
    id?: string;
    object?: string;
    type?: string;
    status?: string;
    metadata?: Record<string, string>;
    usage?: Usage;
    created_at?: number;
    assistant_id?: string;
    thread_id?: string;
    run_id?: string;
    step_details?: StepDetails;
    last_error?: LastError;
    expired_at?: number;
    cancelled_at?: number;
    failed_at?: number;
    completed_at?: number;
    expires_at?: number;
};

