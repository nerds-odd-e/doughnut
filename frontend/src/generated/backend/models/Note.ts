/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { NoteAccessories } from './NoteAccessories';
import type { PictureWithMask } from './PictureWithMask';
export type Note = {
    topic?: string;
    topicConstructor?: string;
    details?: string;
    parentId?: number;
    updatedAt?: string;
    id?: number;
    noteAccessories?: NoteAccessories;
    createdAt?: string;
    readonly deletedAt?: string;
    wikidataId?: string;
    pictureWithMask?: PictureWithMask;
};

