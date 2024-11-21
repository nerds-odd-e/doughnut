/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { ToolCallCodeInterpreter } from './ToolCallCodeInterpreter';
import type { ToolCallFileSearch } from './ToolCallFileSearch';
import type { ToolCallFunction } from './ToolCallFunction';
export type ToolCall = {
    index?: number;
    id?: string;
    type?: string;
    function?: ToolCallFunction;
    code_interpreter?: ToolCallCodeInterpreter;
    file_search?: ToolCallFileSearch;
};

