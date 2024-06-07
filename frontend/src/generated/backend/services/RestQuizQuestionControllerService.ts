/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { AnswerDTO } from '../models/AnswerDTO';
import type { AnsweredQuestion } from '../models/AnsweredQuestion';
import type { QuestionSuggestionCreationParams } from '../models/QuestionSuggestionCreationParams';
import type { QuizQuestion } from '../models/QuizQuestion';
import type { QuizQuestionContestResult } from '../models/QuizQuestionContestResult';
import type { SuggestedQuestionForFineTuning } from '../models/SuggestedQuestionForFineTuning';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class RestQuizQuestionControllerService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * @param quizQuestion
     * @param requestBody
     * @returns SuggestedQuestionForFineTuning OK
     * @throws ApiError
     */
    public suggestQuestionForFineTuning(
        quizQuestion: number,
        requestBody: QuestionSuggestionCreationParams,
    ): CancelablePromise<SuggestedQuestionForFineTuning> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/quiz-questions/{quizQuestion}/suggest-fine-tuning',
            path: {
                'quizQuestion': quizQuestion,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param quizQuestion
     * @returns QuizQuestion OK
     * @throws ApiError
     */
    public regenerate(
        quizQuestion: number,
    ): CancelablePromise<QuizQuestion> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/quiz-questions/{quizQuestion}/regenerate',
            path: {
                'quizQuestion': quizQuestion,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param quizQuestion
     * @returns QuizQuestionContestResult OK
     * @throws ApiError
     */
    public contest(
        quizQuestion: number,
    ): CancelablePromise<QuizQuestionContestResult> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/quiz-questions/{quizQuestion}/contest',
            path: {
                'quizQuestion': quizQuestion,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param quizQuestion
     * @param requestBody
     * @returns AnsweredQuestion OK
     * @throws ApiError
     */
    public answerQuiz(
        quizQuestion: number,
        requestBody: AnswerDTO,
    ): CancelablePromise<AnsweredQuestion> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/quiz-questions/{quizQuestion}/answer',
            path: {
                'quizQuestion': quizQuestion,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param requestBody
     * @returns any OK
     * @throws ApiError
     */
    public reviewQuizQuestion(
        requestBody: Array<QuizQuestion>,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/quiz-questions/review',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param note
     * @returns QuizQuestion OK
     * @throws ApiError
     */
    public generateQuestion(
        note: number,
    ): CancelablePromise<QuizQuestion> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/quiz-questions/generate-question',
            query: {
                'note': note,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param note
     * @returns QuizQuestion OK
     * @throws ApiError
     */
    public getAllQuizQuestionByNote(
        note: number,
    ): CancelablePromise<Array<QuizQuestion>> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/quiz-questions/{note}/note-questions',
            path: {
                'note': note,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param headNote
     * @returns QuizQuestion OK
     * @throws ApiError
     */
    public getAllQuizQuestionByNoteBook(
        headNote: number,
    ): CancelablePromise<Array<QuizQuestion>> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/quiz-questions/{headNote}/note-book-questions',
            path: {
                'headNote': headNote,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
    /**
     * @param headNote
     * @returns QuizQuestion OK
     * @throws ApiError
     */
    public getAllPendingQuizQuestionByNoteBook(
        headNote: number,
    ): CancelablePromise<Array<QuizQuestion>> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/quiz-questions/{headNote}/note-book-pending-questions',
            path: {
                'headNote': headNote,
            },
            errors: {
                500: `Internal Server Error`,
            },
        });
    }
}
