import type { DailyProbeSide } from "@/models/dailyProbe"
import { ref } from "vue"

const PRESS_FLASH_MS = 200

export function useDailyProbePressFlash() {
  const pressedSide = ref<DailyProbeSide | undefined>()
  let pressFlash: ReturnType<typeof setTimeout> | undefined

  function clearPressFlash() {
    if (pressFlash !== undefined) {
      clearTimeout(pressFlash)
      pressFlash = undefined
    }
    pressedSide.value = undefined
  }

  function startPressFlash(side: DailyProbeSide) {
    clearPressFlash()
    pressedSide.value = side
    pressFlash = setTimeout(clearPressFlash, PRESS_FLASH_MS)
  }

  return { pressedSide, startPressFlash, clearPressFlash }
}
