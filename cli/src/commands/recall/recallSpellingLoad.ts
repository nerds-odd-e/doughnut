import {
  MemoryTrackerController,
  RecallPromptController,
  type RecallPrompt,
} from 'doughnut-api'
import {
  doughnutSdkOptions,
  runDefaultBackendJson,
} from '../../backendApi/doughnutBackendClient.js'

/** Spelling memory tracker: server spelling question first (same order as web recall). */
export type SpellingRecallSessionPayload = {
  readonly memoryTrackerId: number
  readonly notebookTitle?: string
}

export async function fetchSpellingRecallPrompt(
  memoryTrackerId: number,
  signal?: AbortSignal
): Promise<{ readonly recallPromptId: number; readonly stemMarkdown: string }> {
  const prompt = await runDefaultBackendJson<RecallPrompt>(() =>
    MemoryTrackerController.askAQuestion({
      path: { memoryTracker: memoryTrackerId },
      ...doughnutSdkOptions(signal),
    })
  )
  if (prompt.questionType !== 'SPELLING') {
    throw new Error('Expected a spelling recall prompt from the server.')
  }
  const recallPromptId = prompt.id
  if (recallPromptId === undefined) {
    throw new Error('Spelling recall prompt has no id.')
  }
  return {
    recallPromptId,
    stemMarkdown: prompt.spellingQuestion?.stem ?? '',
  }
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
