import { ref, computed, onMounted, onBeforeUnmount } from "vue"

export function useBreakpoint() {
  const isLgOrLarger = ref(false)
  let mediaQuery: MediaQueryList | null = null

  const handleMediaChange = (event: MediaQueryListEvent) => {
    isLgOrLarger.value = event.matches
  }

  onMounted(() => {
    // Match CSS @media (max-width: theme('screens.lg'))
    // CSS uses max-width: 1024px for tablet, so we need min-width: 1025px for desktop
    // This ensures at exactly 1024px, both JS and CSS treat it as tablet
    // Read the actual breakpoint from CSS custom property or use 1025px to match CSS behavior
    const lgBreakpoint = 1024
    mediaQuery = window.matchMedia(`(min-width: ${lgBreakpoint + 1}px)`)
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
