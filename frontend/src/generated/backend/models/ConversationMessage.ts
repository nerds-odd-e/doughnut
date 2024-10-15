/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Conversation } from './Conversation';
import type { User } from './User';
export type ConversationMessage = {
    id: number;
    conversation?: Conversation;
    message: string;
    sender?: User;
    is_read: boolean;
    createdAt: string;
};

