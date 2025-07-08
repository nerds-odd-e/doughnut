/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { DeltaOfRunStep } from './DeltaOfRunStep';
import type { Message } from './Message';
import type { MessageDelta } from './MessageDelta';
import type { NoteDetailsCompletion } from './NoteDetailsCompletion';
import type { Run } from './Run';
import type { RunStep } from './RunStep';
import type { TitleReplacement } from './TitleReplacement';
export type DummyForGeneratingTypes = {
    message?: Message;
    runStep?: RunStep;
    runStepDelta?: DeltaOfRunStep;
    messageDelta?: MessageDelta;
    run?: Run;
    noteDetailsCompletion?: NoteDetailsCompletion;
    titleReplacement?: TitleReplacement;
    aiToolName?: DummyForGeneratingTypes.aiToolName;
};
export namespace DummyForGeneratingTypes {
    export enum aiToolName {
        COMPLETE_NOTE_DETAILS = 'complete_note_details',
        SUGGEST_NOTE_TITLE = 'suggest_note_title',
        ASK_SINGLE_ANSWER_MULTIPLE_CHOICE_QUESTION = 'ask_single_answer_multiple_choice_question',
    }
}

