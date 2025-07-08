/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Attachment } from './Attachment';
import type { IncompleteDetails } from './IncompleteDetails';
import type { MessageContent } from './MessageContent';
export type Message = {
    id?: string;
    object?: string;
    status?: string;
    role?: string;
    content?: Array<MessageContent>;
    attachments?: Array<Attachment>;
    metadata?: Record<string, string>;
    created_at?: number;
    thread_id?: string;
    incomplete_details?: IncompleteDetails;
    completed_at?: number;
    incomplete_at?: number;
    assistant_id?: string;
    run_id?: string;
};

