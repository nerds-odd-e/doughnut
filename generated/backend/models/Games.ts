/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Rounds } from './Rounds';
export type Games = {
    id: number;
    numberOfPlayers?: number;
    winner?: string;
    maxSteps?: number;
    createdDate?: string;
    endDate?: string;
    updatedDate?: string;
    rounds?: Array<Rounds>;
};

