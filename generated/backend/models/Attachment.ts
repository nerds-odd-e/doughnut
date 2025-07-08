/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { CodeInterpreterTool } from './CodeInterpreterTool';
import type { FileSearchTool } from './FileSearchTool';
import type { FunctionTool } from './FunctionTool';
export type Attachment = {
    tools?: Array<(CodeInterpreterTool | FileSearchTool | FunctionTool)>;
    file_id?: string;
};

