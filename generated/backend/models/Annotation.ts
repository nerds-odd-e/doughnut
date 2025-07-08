/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { FileCitation } from './FileCitation';
import type { FilePath } from './FilePath';
export type Annotation = {
    index?: number;
    type?: string;
    text?: string;
    file_path?: FilePath;
    file_citation?: FileCitation;
    start_index?: number;
    end_index?: number;
};

