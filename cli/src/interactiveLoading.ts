import type { OutputAdapter } from './types.js'

export const LOADING_MESSAGES = {
  recallNext: 'Loading recall questions',
} as const

let loadingState: { message: string } | null = null

export function getInteractiveLoadingDisplayMessage(): string | null {
  return loadingState?.message ?? null
}

export function applyInteractiveLoading(
  output: OutputAdapter,
  state: { message: string } | null
): void {
  loadingState = state
  output.notifyLoadingChanged?.()
}

export function resetInteractiveLoadingForTesting(): void {
  loadingState = null
}
