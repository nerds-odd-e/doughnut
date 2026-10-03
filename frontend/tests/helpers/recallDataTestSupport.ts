import { useRecallData } from "@/composables/useRecallData"

/** Recall state is module-level; reset it so tests do not leak into each other. */
export function resetRecallData() {
  const recallData = useRecallData()
  recallData.setToRepeat(undefined)
  recallData.setDueCommissioned(undefined)
  recallData.setCurrentRecallWindowEndAt(undefined)
  recallData.setTotalAssimilatedCount(undefined)
  recallData.setIsRecallPaused(false)
  recallData.setIsViewingAnsweredQuestion(false)
  recallData.clearShouldResumeRecall()
  recallData.setTreadmillMode(false)
  recallData.setCurrentIndex(0)
  recallData.setDiligentMode(false)
}
