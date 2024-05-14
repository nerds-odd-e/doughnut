/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Audio } from './Audio';
import type { ImageWithMask } from './ImageWithMask';
export type NoteAccessory = {
    id: number;
    url?: string;
    imageUrl?: string;
    imageMask?: string;
    useParentImage?: boolean;
    audioAttachment?: Audio;
    imageWithMask?: ImageWithMask;
};

