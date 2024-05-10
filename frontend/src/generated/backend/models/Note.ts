/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { NoteAccessory } from './NoteAccessory';
export type Note = {
    topic: string;
    topicConstructor: string;
    details?: string;
    parentId?: number;
    updatedAt: string;
    id: number;
    noteAccessory?: NoteAccessory;
    createdAt: string;
    readonly deletedAt?: string;
    wikidataId?: string;
};

