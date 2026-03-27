import type { RecallPrompt } from 'doughnut-api'
import type { RecallNextResult } from '../src/commands/recall.js'

export function recallNextQuestion(prompt: RecallPrompt): RecallNextResult {
  return { type: 'question', prompt }
}
