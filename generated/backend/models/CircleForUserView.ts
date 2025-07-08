/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { NotebooksViewedByUser } from './NotebooksViewedByUser';
import type { UserForOtherUserView } from './UserForOtherUserView';
export type CircleForUserView = {
    id: number;
    name: string;
    invitationCode: string;
    notebooks: NotebooksViewedByUser;
    members: Array<UserForOtherUserView>;
};

