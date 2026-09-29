import { RecallsController } from "@generated/donut-backend-api/sdk.gen"
import timezoneParam from "@/managedApi/window/timezoneParam"
import { useRecallData } from "@/composables/useRecallData"

export const fetchDueRecalls = async (dueInDays?: number) => {
  const { data: response, error } = await RecallsController.recalling({
    query: { timezone: timezoneParam(), dueindays: dueInDays },
  })
  if (!error && response) return response
  return
}

export function useRecallCatchUp() {
  const {
    toRepeat,
    setToRepeat,
    setDueCommissioned,
    setTotalAssimilatedCount,
    setCurrentRecallWindowEndAt,
  } = useRecallData()

  const catchUpDueRecalls = async () => {
    const response = await fetchDueRecalls(0)
    if (!response) return
    const queued = toRepeat.value ?? []
    const queuedIds = new Set(queued.map((t) => t.memoryTrackerId))
    setToRepeat([
      ...queued,
      ...(response.toRepeat ?? []).filter(
        (t) => !queuedIds.has(t.memoryTrackerId)
      ),
    ])
    setDueCommissioned(response.dueCommissioned ?? [])
    setTotalAssimilatedCount(response.totalAssimilatedCount)
    setCurrentRecallWindowEndAt(response.currentRecallWindowEndAt)
  }

  return { catchUpDueRecalls }
}
