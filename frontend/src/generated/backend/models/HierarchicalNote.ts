/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { NoteAccessories } from './NoteAccessories';
import type { PictureWithMask } from './PictureWithMask';
export type HierarchicalNote = {
    topic: string;
    topicConstructor: string;
    details?: string;
    parentId?: number;
    updatedAt: string;
    id: number;
    noteAccessories: NoteAccessories;
    createdAt: string;
    readonly deletedAt?: string;
    wikidataId?: string;
    pictureWithMask?: PictureWithMask;
};

