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
    currentIndex,
    setToRepeat,
    setDueCommissioned,
    setTotalAssimilatedCount,
    setCurrentRecallWindowEndAt,
    requestDueRecallsRefresh,
  } = useRecallData()

  // Adds newly due trackers after the queue without moving the position; only
  // trackers still waiting count as queued, so an answered one due again is
  // added. Explicit refreshes (after assimilating or removing) replace instead.
  const catchUpDueRecalls = async () => {
    const response = await fetchDueRecalls(0)
    if (!response) return
    const queued = toRepeat.value ?? []
    const waitingIds = new Set(
      queued.slice(currentIndex.value).map((t) => t.memoryTrackerId)
    )
    setToRepeat([
      ...queued,
      ...(response.toRepeat ?? []).filter(
        (t) => !waitingIds.has(t.memoryTrackerId)
      ),
    ])
    setDueCommissioned(response.dueCommissioned ?? [])
    setTotalAssimilatedCount(response.totalAssimilatedCount)
    setCurrentRecallWindowEndAt(response.currentRecallWindowEndAt)
  }

  // Replaces today's due queue after due times change elsewhere: a kept-alive
  // recall page reloads on the refresh request; otherwise catching up refills
  // the emptied queue.
  const replaceDueRecalls = async () => {
    setToRepeat(undefined)
    requestDueRecallsRefresh()
    await catchUpDueRecalls()
  }

  return { catchUpDueRecalls, replaceDueRecalls }
}
