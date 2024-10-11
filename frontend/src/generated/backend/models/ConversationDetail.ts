/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Conversation } from './Conversation';
import type { User } from './User';
export type ConversationDetail = {
    id: number;
    conversation?: Conversation;
    message: string;
    conversationDetailInitiator?: User;
    createdAt: string;
};

