import { AssimilationController } from "@generated/doughnut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import timezoneParam from "@/managedApi/window/timezoneParam"
import { useAssimilationCount } from "@/composables/useAssimilationCount"
import { useAssimilationView } from "@/composables/useAssimilationView"
import { useToast } from "@/composables/useToast"
import { noteShowLocation } from "@/routes/noteShowLocation"
import { useRouter } from "vue-router"

export const DAILY_GOAL_TOAST = "You've achieved your daily assimilation goal"
export const NO_MORE_TOAST = "No more notes to assimilate"

export function useGoToNextAssimilation() {
  const router = useRouter()
  const { openForNote, dismiss } = useAssimilationView()
  const { applyAssimilationCountDto } = useAssimilationCount()
  const { showSuccessToast } = useToast()

  const goToNextAssimilation = async (): Promise<boolean> => {
    const { data, error } = await apiCallWithLoading(() =>
      AssimilationController.next({
        query: { timezone: timezoneParam() },
      })
    )

    if (error || !data) {
      return false
    }

    applyAssimilationCountDto(data.counts)

    const { nextUnit } = data
    if (nextUnit?.noteId == null) {
      showSuccessToast(NO_MORE_TOAST)
      dismiss()
      return false
    }

    if (data.counts?.dueCount === 0) {
      showSuccessToast(DAILY_GOAL_TOAST)
    }

    const { noteId, propertyKey } = nextUnit
    openForNote(noteId, propertyKey)
    await router.push(noteShowLocation(noteId))
    return true
  }

  return { goToNextAssimilation }
}
