import { RecallPromptController, type RecallPrompt } from 'doughnut-api'
import {
  doughnutSdkOptions,
  runDefaultBackendJson,
} from '../../backendApi/doughnutBackendClient.js'

export type RecallSpellingCardPayload = {
  readonly memoryTrackerId: number
  readonly recallPromptId: number
  readonly stemMarkdown: string
  readonly notebookTitle?: string
}

export async function submitSpellingAnswer(
  recallPromptId: number,
  spellingAnswer: string,
  signal?: AbortSignal
): Promise<RecallPrompt> {
  return runDefaultBackendJson<RecallPrompt>(() =>
    RecallPromptController.answerSpelling({
      path: { recallPrompt: recallPromptId },
      body: { spellingAnswer },
      ...doughnutSdkOptions(signal),
    })
  )
}
