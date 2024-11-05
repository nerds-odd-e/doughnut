/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Circle } from './Circle';
import type { NotebookSettings } from './NotebookSettings';
import type { NoteTopic } from './NoteTopic';
export type Notebook = {
    id: number;
    certifiable?: boolean;
    notebookSettings: NotebookSettings;
    creatorId?: string;
    headNoteTopic: NoteTopic;
    circle?: Circle;
    updated_at: string;
};

