/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { McpAddNoteResponseDTO } from '../models/McpAddNoteResponseDTO';
import type { McpNoteAddDTO } from '../models/McpNoteAddDTO';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class McpNoteCreationControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @param requestBody
     * @returns McpAddNoteResponseDTO OK
     * @throws ApiError
     */
    public createNote1(
        requestBody: McpNoteAddDTO,
    ): CancelablePromise<McpAddNoteResponseDTO> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/mcp/notes/create',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
