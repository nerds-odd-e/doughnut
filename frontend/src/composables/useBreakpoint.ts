import { ref, computed, onMounted, onBeforeUnmount } from "vue"

export function useBreakpoint() {
  const isLgOrLarger = ref(false)
  let mediaQuery: MediaQueryList | null = null

  const handleMediaChange = (event: MediaQueryListEvent) => {
    isLgOrLarger.value = event.matches
  }

  onMounted(() => {
    // Match CSS @media (max-width: theme('screens.lg'))
    // lg breakpoint is 1024px, so min-width: 1024px means lg or larger
    mediaQuery = window.matchMedia("(min-width: 1024px)")
    isLgOrLarger.value = mediaQuery.matches

    // Modern browsers support addEventListener
    if (mediaQuery.addEventListener) {
      mediaQuery.addEventListener("change", handleMediaChange)
    } else {
      // Fallback for older browsers
      mediaQuery.addListener(handleMediaChange)
    }
  })

  onBeforeUnmount(() => {
    if (mediaQuery) {
      if (mediaQuery.removeEventListener) {
        mediaQuery.removeEventListener("change", handleMediaChange)
      } else {
        // Fallback for older browsers
        mediaQuery.removeListener(handleMediaChange)
      }
    }
  })

  return {
    isLgOrLarger: computed(() => isLgOrLarger.value),
  }
}
