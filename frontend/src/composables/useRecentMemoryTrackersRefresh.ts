import { ref } from "vue"

const recentMemoryTrackersRefreshTick = ref(0)

export function requestRecentMemoryTrackersRefresh() {
  recentMemoryTrackersRefreshTick.value += 1
}

export function useRecentMemoryTrackersRefreshTick() {
  return { recentMemoryTrackersRefreshTick }
}
