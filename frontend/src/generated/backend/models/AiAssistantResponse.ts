/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { AiCompletionRequiredAction } from './AiCompletionRequiredAction';
import type { Message } from './Message';
export type AiAssistantResponse = {
    threadId?: string;
    runId?: string;
    requiredAction?: AiCompletionRequiredAction;
    messages?: Array<Message>;
};

