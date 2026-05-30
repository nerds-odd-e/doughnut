import { computed, watch, type Ref } from "vue"
import type { MemoryTrackerLite } from "@generated/doughnut-backend-api"

interface RecallTrackerNavigationDeps {
  toRepeat: Ref<MemoryTrackerLite[] | undefined>
  currentIndex: Ref<number>
  treadmillMode: Ref<boolean>
  setToRepeat: (trackers: MemoryTrackerLite[] | undefined) => void
}

export function useRecallTrackerNavigation({
  toRepeat,
  currentIndex,
  treadmillMode,
  setToRepeat,
}: RecallTrackerNavigationDeps) {
  const moveToFirstNonSpellingFromCurrent = () => {
    if (!toRepeat.value) return
    const currentTracker = toRepeat.value[currentIndex.value]
    if (currentTracker === undefined || !currentTracker.spelling) return
    const firstNonSpelling = toRepeat.value.findIndex(
      (t, idx) => !t.spelling && idx >= currentIndex.value
    )
    if (firstNonSpelling !== -1) {
      currentIndex.value = firstNonSpelling
    }
  }

  const getCurrentMemoryTracker = (): MemoryTrackerLite | undefined => {
    if (!toRepeat.value) return
    if (!treadmillMode.value) {
      return toRepeat.value[currentIndex.value]
    }
    for (let i = currentIndex.value; i < toRepeat.value.length; i++) {
      const t = toRepeat.value[i]!
      if (!t.spelling) {
        return t
      }
    }
    return
  }

  const getCurrentMemoryTrackerIndex = (): number => {
    if (!toRepeat.value) return 0
    if (!treadmillMode.value) return currentIndex.value
    let index = currentIndex.value
    while (index < toRepeat.value.length && toRepeat.value[index]!.spelling) {
      index++
    }
    return index < toRepeat.value.length ? index : currentIndex.value
  }

  const nextIsSpelling = computed(() => {
    if (treadmillMode.value) return false
    return toRepeat.value?.[currentIndex.value + 1]?.spelling ?? false
  })

  const toRepeatCount = computed(() => {
    if (!toRepeat.value) return 0
    if (!treadmillMode.value) {
      return toRepeat.value.length - currentIndex.value
    }
    let count = 0
    for (let i = currentIndex.value; i < toRepeat.value.length; i++) {
      if (!toRepeat.value[i]!.spelling) {
        count++
      }
    }
    return count
  })

  const moveToNextMemoryTracker = () => {
    if (!toRepeat.value) return
    if (!treadmillMode.value) {
      currentIndex.value += 1
      return
    }
    let nextIndex = currentIndex.value + 1
    while (
      nextIndex < toRepeat.value.length &&
      toRepeat.value[nextIndex]!.spelling
    ) {
      nextIndex += 1
    }
    currentIndex.value = nextIndex
  }

  const moveMemoryTrackerToEnd = (index: number) => {
    const currentToRepeat = toRepeat.value
    if (!currentToRepeat) return
    const item = currentToRepeat[index]
    if (item === undefined) return
    setToRepeat([
      ...currentToRepeat.slice(0, index),
      ...currentToRepeat.slice(index + 1),
      item,
    ])
  }

  // When treadmill mode turns off, move unanswered spelling trackers to the end
  // so they are revisited after the remaining non-spelling trackers.
  const deferUnansweredSpellingTrackers = () => {
    if (!toRepeat.value) return
    const unansweredSpellingTrackers: MemoryTrackerLite[] = []
    const nonSpellingTrackers: MemoryTrackerLite[] = []
    for (let i = currentIndex.value; i < toRepeat.value.length; i++) {
      const tracker = toRepeat.value[i]
      if (!tracker) continue
      if (tracker.spelling) {
        unansweredSpellingTrackers.push(tracker)
      } else {
        nonSpellingTrackers.push(tracker)
      }
    }
    if (unansweredSpellingTrackers.length > 0) {
      setToRepeat([
        ...toRepeat.value.slice(0, currentIndex.value),
        ...nonSpellingTrackers,
        ...unansweredSpellingTrackers,
      ])
    }
  }

  const handleTreadmillModeChanged = () => {
    if (!toRepeat.value) return
    if (treadmillMode.value) {
      moveToFirstNonSpellingFromCurrent()
    } else {
      deferUnansweredSpellingTrackers()
    }
  }

  watch(
    () => treadmillMode.value,
    () => {
      if (treadmillMode.value) {
        moveToFirstNonSpellingFromCurrent()
      }
    }
  )

  return {
    nextIsSpelling,
    toRepeatCount,
    getCurrentMemoryTracker,
    getCurrentMemoryTrackerIndex,
    moveToNextMemoryTracker,
    moveMemoryTrackerToEnd,
    handleTreadmillModeChanged,
  }
}
