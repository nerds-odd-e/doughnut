/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { ChatResponseFormat } from './ChatResponseFormat';
import type { CodeInterpreterTool } from './CodeInterpreterTool';
import type { FileSearchTool } from './FileSearchTool';
import type { FunctionTool } from './FunctionTool';
import type { IncompleteDetails } from './IncompleteDetails';
import type { LastError } from './LastError';
import type { RequiredAction } from './RequiredAction';
import type { ToolChoice } from './ToolChoice';
import type { ToolResources } from './ToolResources';
import type { TruncationStrategy } from './TruncationStrategy';
import type { Usage } from './Usage';
export type Run = {
    id?: string;
    object?: string;
    status?: string;
    model?: string;
    instructions?: string;
    tools?: Array<(CodeInterpreterTool | FileSearchTool | FunctionTool)>;
    metadata?: Record<string, string>;
    usage?: Usage;
    temperature?: number;
    created_at?: number;
    thread_id?: string;
    assistant_id?: string;
    required_action?: RequiredAction;
    last_error?: LastError;
    expires_at?: number;
    started_at?: number;
    cancelled_at?: number;
    failed_at?: number;
    completed_at?: number;
    incomplete_details?: IncompleteDetails;
    top_p?: number;
    max_prompt_tokens?: number;
    max_completion_tokens?: number;
    truncation_strategy?: TruncationStrategy;
    tool_choice?: ToolChoice;
    response_format?: ChatResponseFormat;
    tool_resources?: ToolResources;
};

