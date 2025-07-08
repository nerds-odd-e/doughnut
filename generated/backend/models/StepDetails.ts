/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { MessageCreation } from './MessageCreation';
import type { ToolCall } from './ToolCall';
export type StepDetails = {
    type?: string;
    message_creation?: MessageCreation;
    tool_calls?: Array<ToolCall>;
};

