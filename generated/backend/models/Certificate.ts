/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Notebook } from './Notebook';
import type { User } from './User';
export type Certificate = {
    id: number;
    user?: User;
    notebook?: Notebook;
    startDate: string;
    expiryDate: string;
    creatorName?: string;
};

