import type { User } from "@generated/donut-backend-api"
import { DailyProbeController } from "@generated/donut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import timezoneParam from "@/managedApi/window/timezoneParam"
import { computed, ref, watch, type Ref } from "vue"

export function useDailyProbeOffer(
  currentUser: Ref<User | undefined> | undefined
) {
  const completedToday = ref<boolean | undefined>(undefined)
  const offerCheckFailed = ref(false)
  const enabled = computed(() => !!currentUser?.value?.dailyProbeEnabled)

  const checkOffer = async () => {
    if (!enabled.value) return
    const { data, error } = await apiCallWithLoading(() =>
      DailyProbeController.getDailyProbeToday({
        query: { timezone: timezoneParam() },
      })
    )
    if (error) {
      offerCheckFailed.value = true
      return
    }
    offerCheckFailed.value = false
    completedToday.value = data.completed
  }

  watch(
    enabled,
    async (isEnabled) => {
      if (!isEnabled || completedToday.value !== undefined) return
      await checkOffer()
    },
    { immediate: true }
  )

  const showDailyProbe = computed(
    () => enabled.value && completedToday.value === false
  )
  const showOrdinaryRecall = computed(
    () => !enabled.value || completedToday.value === true
  )
  const showOfferRetry = computed(() => enabled.value && offerCheckFailed.value)

  return {
    showDailyProbe,
    showOrdinaryRecall,
    showOfferRetry,
    checkOffer,
    markDailyProbeFinished: () => {
      completedToday.value = true
    },
  }
}
