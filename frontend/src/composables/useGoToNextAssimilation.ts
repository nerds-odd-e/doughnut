import { AssimilationController } from "@generated/doughnut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import timezoneParam from "@/managedApi/window/timezoneParam"
import { useAssimilationCount } from "@/composables/useAssimilationCount"
import { useAssimilationView } from "@/composables/useAssimilationView"
import { useToast } from "@/composables/useToast"
import { noteShowLocation } from "@/routes/noteShowLocation"
import { useRouter } from "vue-router"

const DAILY_GOAL_TOAST = "You've achieved your daily assimilation goal"
const NO_MORE_TOAST = "No more notes to assimilate"

export function useGoToNextAssimilation() {
  const router = useRouter()
  const { requestOnFor } = useAssimilationView()
  const { applyAssimilationCountDto } = useAssimilationCount()
  const { showSuccessToast } = useToast()

  const goToNextAssimilation = async () => {
    const { data, error } = await apiCallWithLoading(() =>
      AssimilationController.next({
        query: { timezone: timezoneParam() },
      })
    )

    if (error || !data) {
      return
    }

    applyAssimilationCountDto(data.counts)

    if (data.nextNoteId == null) {
      showSuccessToast(NO_MORE_TOAST)
      return
    }

    if (data.counts?.dueCount === 0) {
      showSuccessToast(DAILY_GOAL_TOAST)
    }

    requestOnFor(data.nextNoteId)
    await router.push(noteShowLocation(data.nextNoteId))
  }

  return { goToNextAssimilation }
}
