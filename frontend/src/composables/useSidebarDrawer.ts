import { computed, onBeforeUnmount, onMounted, ref } from "vue"

const DRAWER_BREAKPOINT_PX = 768

/**
 * Open state of a side panel that sits beside the page from 768 px up and is a drawer below
 * that. Decided synchronously so the panel has its final width before the page content mounts.
 */
export function useSidebarDrawer() {
  const windowWidth = ref(window.innerWidth)
  const isMdOrLarger = computed(() => windowWidth.value >= DRAWER_BREAKPOINT_PX)
  const opened = ref(isMdOrLarger.value)

  function handleResize() {
    windowWidth.value = window.innerWidth
  }

  onMounted(() => window.addEventListener("resize", handleResize))
  onBeforeUnmount(() => window.removeEventListener("resize", handleResize))

  function closeOnPhone() {
    if (!isMdOrLarger.value) {
      opened.value = false
    }
  }

  return { opened, isMdOrLarger, closeOnPhone }
}
