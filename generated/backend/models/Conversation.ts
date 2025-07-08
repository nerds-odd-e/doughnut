/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { ConversationSubject } from './ConversationSubject';
import type { Ownership } from './Ownership';
import type { User } from './User';
export type Conversation = {
    id: number;
    subject?: ConversationSubject;
    subjectOwnership?: Ownership;
    conversationInitiator?: User;
    createdAt: string;
    updatedAt: string;
    aiAssistantThreadId?: string;
    lastAiAssistantThreadSync?: string;
};

