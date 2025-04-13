/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Circle } from './Circle';
import type { NotebookSettings } from './NotebookSettings';
import type { User } from './User';
export type Notebook = {
    id: number;
    certifiable?: boolean;
    notebookSettings: NotebookSettings;
    creatorId?: string;
    title: string;
    circle?: Circle;
    headNoteId: number;
    shortDetails?: string;
    updated_at: string;
    owner?: User;
};

