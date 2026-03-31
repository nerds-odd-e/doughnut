/** Past-assistant line when a just-review recall session ends (load-more declined or extended window empty). */
export function recallSessionSummaryLine(recalled: number): string {
  return recalled === 1 ? 'Recalled 1 note' : `Recalled ${recalled} notes`
}
