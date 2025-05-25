/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { WikidataEntityData } from '../models/WikidataEntityData';
import type { WikidataSearchEntity } from '../models/WikidataSearchEntity';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestWikidataControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @param search
     * @returns WikidataSearchEntity OK
     * @throws ApiError
     */
    public searchWikidata(
        search: string,
    ): CancelablePromise<Array<WikidataSearchEntity>> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/wikidata/search/{search}',
            path: {
                'search': search,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param wikidataId
     * @returns WikidataEntityData OK
     * @throws ApiError
     */
    public fetchWikidataEntityDataById(
        wikidataId: string,
    ): CancelablePromise<WikidataEntityData> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/wikidata/entity-data/{wikidataId}',
            path: {
                'wikidataId': wikidataId,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
