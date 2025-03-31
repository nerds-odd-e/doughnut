/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { CompletionTokensDetails } from './CompletionTokensDetails';
import type { PromptTokensDetails } from './PromptTokensDetails';
export type Usage = {
    prompt_tokens?: number;
    completion_tokens?: number;
    total_tokens?: number;
    prompt_tokens_details?: PromptTokensDetails;
    completion_tokens_details?: CompletionTokensDetails;
};

