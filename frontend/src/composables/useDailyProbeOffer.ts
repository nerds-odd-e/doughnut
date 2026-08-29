import type { User } from "@generated/donut-backend-api"
import { DailyProbeController } from "@generated/donut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import timezoneParam from "@/managedApi/window/timezoneParam"
import { computed, ref, watch, type Ref } from "vue"

export function useDailyProbeOffer(
  currentUser: Ref<User | undefined> | undefined
) {
  const completedToday = ref<boolean | undefined>(undefined)
  const enabled = computed(() => !!currentUser?.value?.dailyProbeEnabled)

  watch(
    enabled,
    async (isEnabled) => {
      if (!isEnabled || completedToday.value !== undefined) return
      const { data, error } = await apiCallWithLoading(() =>
        DailyProbeController.getDailyProbeToday({
          query: { timezone: timezoneParam() },
        })
      )
      if (!error) {
        completedToday.value = data!.completed
      }
    },
    { immediate: true }
  )

  const showDailyProbe = computed(
    () => enabled.value && completedToday.value === false
  )
  const showOrdinaryRecall = computed(
    () => !enabled.value || completedToday.value === true
  )

  return {
    showDailyProbe,
    showOrdinaryRecall,
    markDailyProbeFinished: () => {
      completedToday.value = true
    },
  }
}
