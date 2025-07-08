/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { User } from './User';
export type ConversationMessage = {
    id: number;
    message: string;
    sender?: User;
    readByReceiver?: boolean;
    createdAt?: string;
};

