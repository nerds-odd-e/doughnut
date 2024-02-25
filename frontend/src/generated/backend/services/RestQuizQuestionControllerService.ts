/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Answer } from '../models/Answer';
import type { AnsweredQuestion } from '../models/AnsweredQuestion';
import type { Note } from '../models/Note';
import type { QuestionSuggestionCreationParams } from '../models/QuestionSuggestionCreationParams';
import type { QuizQuestion } from '../models/QuizQuestion';
import type { QuizQuestionAIQuestion } from '../models/QuizQuestionAIQuestion';
import type { QuizQuestionContestResult } from '../models/QuizQuestionContestResult';
import type { QuizQuestionEntity } from '../models/QuizQuestionEntity';
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
        quizQuestion: QuizQuestionAIQuestion,
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
        quizQuestion: QuizQuestionEntity,
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
        quizQuestion: QuizQuestionAIQuestion,
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
        quizQuestion: QuizQuestionEntity,
        requestBody: Answer,
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
     * @param note
     * @returns QuizQuestion OK
     * @throws ApiError
     */
    public generateQuestion(
        note: Note,
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
}
