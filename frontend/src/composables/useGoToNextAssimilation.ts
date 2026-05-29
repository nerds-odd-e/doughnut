import { AssimilationController } from "@generated/doughnut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import timezoneParam from "@/managedApi/window/timezoneParam"
import { useAssimilationCount } from "@/composables/useAssimilationCount"
import { useAssimilationView } from "@/composables/useAssimilationView"
import { noteShowLocation } from "@/routes/noteShowLocation"
import { useRouter } from "vue-router"

export function useGoToNextAssimilation() {
  const router = useRouter()
  const { requestOnFor } = useAssimilationView()
  const { applyAssimilationCountDto } = useAssimilationCount()

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
      return
    }

    requestOnFor(data.nextNoteId)
    await router.push(noteShowLocation(data.nextNoteId))
  }

  return { goToNextAssimilation }
}
