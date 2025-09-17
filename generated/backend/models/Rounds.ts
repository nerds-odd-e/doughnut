/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Games } from './Games';
import type { Players } from './Players';
export type Rounds = {
    id: number;
    player?: Players;
    game?: Games;
    roundNo?: number;
    mode?: string;
    dice?: number;
    damage?: number;
    step?: number;
    createDate?: string;
    updateDate?: string;
};

