/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Notebook } from './Notebook';
import type { User } from './User';
export type NotebookAssistant = {
    id: number;
    creator: User;
    notebook: Notebook;
    assistantId: string;
    createdAt: string;
};

