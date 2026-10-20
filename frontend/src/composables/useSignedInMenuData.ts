import type { User } from "@generated/donut-backend-api"
import { UserController } from "@generated/donut-backend-api/sdk.gen"
import { onMounted, onUnmounted, watch, type Ref } from "vue"
import { useAssimilationCount } from "@/composables/useAssimilationCount"
import { useRecallCatchUp } from "@/composables/useRecallCatchUp"
import { useRecallData } from "@/composables/useRecallData"
import timezoneParam from "@/managedApi/window/timezoneParam"
import { messageCenter } from "@/store/messageCenter"

// Tolerates a browser clock slightly ahead of the server.
const recallWindowEndMarginMs = 5000
const recallCatchUpAt = (windowEndAt: string) =>
  new Date(windowEndAt).getTime() + recallWindowEndMarginMs

export function useSignedInMenuData(user: Ref<User | undefined>) {
  const { applyAssimilationCountDto } = useAssimilationCount()
  const {
    toRepeat,
    currentRecallWindowEndAt,
    setToRepeat,
    setDueCommissioned,
    setCurrentRecallWindowEndAt,
    setTotalAssimilatedCount,
  } = useRecallData()
  const { catchUpDueRecalls } = useRecallCatchUp()

  const fetchMenuData = async () => {
    const { data: menuData, error } = await UserController.getMenuData({
      query: { timezone: timezoneParam() },
    })
    if (!error && menuData) {
      applyAssimilationCountDto(menuData.assimilationCount)
      if (menuData.recallStatus) {
        if ((toRepeat.value?.length ?? 0) === 0) {
          setToRepeat(menuData.recallStatus.toRepeat)
        }
        setDueCommissioned(menuData.recallStatus.dueCommissioned)
        if (!currentRecallWindowEndAt.value) {
          setCurrentRecallWindowEndAt(
            menuData.recallStatus.currentRecallWindowEndAt
          )
        }
        setTotalAssimilatedCount(menuData.recallStatus.totalAssimilatedCount)
      }
      if (menuData.unreadMessages !== undefined) {
        messageCenter.unreadMessages = menuData.unreadMessages
      }
    }
  }

  watch(
    user,
    () => {
      if (user.value) {
        fetchMenuData()
      }
    },
    { immediate: true }
  )

  let recallCatchUpTimer: ReturnType<typeof setTimeout> | undefined

  watch(
    currentRecallWindowEndAt,
    (endAt) => {
      clearTimeout(recallCatchUpTimer)
      if (!endAt) return
      recallCatchUpTimer = setTimeout(
        catchUpDueRecalls,
        recallCatchUpAt(endAt) - Date.now()
      )
    },
    { immediate: true }
  )

  const catchUpIfWindowEnded = () => {
    const endAt = currentRecallWindowEndAt.value
    if (
      document.visibilityState === "visible" &&
      endAt &&
      Date.now() > recallCatchUpAt(endAt)
    ) {
      clearTimeout(recallCatchUpTimer)
      catchUpDueRecalls()
    }
  }

  onMounted(() =>
    document.addEventListener("visibilitychange", catchUpIfWindowEnded)
  )
  onUnmounted(() => {
    clearTimeout(recallCatchUpTimer)
    document.removeEventListener("visibilitychange", catchUpIfWindowEnded)
  })
}
