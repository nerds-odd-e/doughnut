/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { NotebookSettings } from './NotebookSettings';
import type { NoteTopic } from './NoteTopic';
export type Notebook = {
    id: number;
    certifiable?: boolean;
    notebookSettings: NotebookSettings;
    creatorId?: string;
    updated_at: string;
    headNoteTopic: NoteTopic;
};

