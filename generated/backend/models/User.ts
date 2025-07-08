/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Ownership } from './Ownership';
export type User = {
    id: number;
    name: string;
    externalIdentifier: string;
    ownership?: Ownership;
    dailyAssimilationCount?: number;
    spaceIntervals?: string;
    admin?: boolean;
};

