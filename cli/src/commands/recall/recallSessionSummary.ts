/** Past-assistant line when a just-review recall session ends (load-more declined or extended window empty). */
export function recallSessionSummaryLine(successfulRecalls: number): string {
  return successfulRecalls === 1
    ? 'Recalled 1 note'
    : `Recalled ${successfulRecalls} notes`
}
