import { provideNotebookSidebarOpened } from "@/composables/notebookSidebarOpened"
import { computed, onBeforeUnmount, onMounted, ref, watch, type Ref } from "vue"
import type { RouteLocationNormalizedLoaded } from "vue-router"

const SIDEBAR_BREAKPOINT_PX = 768

export function useNotebookSidebarDrawer(
  route: RouteLocationNormalizedLoaded,
  currentNotebookId: Ref<number | undefined>
) {
  const sidebarOpened = ref(false)
  provideNotebookSidebarOpened(sidebarOpened)

  const windowWidth = ref(
    typeof window !== "undefined" ? window.innerWidth : 1024
  )

  const isMdOrLarger = computed(
    () => windowWidth.value >= SIDEBAR_BREAKPOINT_PX
  )

  const desktopSidebarClass = computed(() =>
    sidebarOpened.value ? "relative" : "hidden"
  )

  const mobileSidebarClass = computed(() => [
    "notebook-sidebar-drawer fixed left-0 z-40",
    sidebarOpened.value ? "translate-x-0" : "-translate-x-full",
  ])

  const sidebarClasses = computed(() => [
    "bg-base-200 w-72 transition-all ease-in-out flex flex-col overflow-x-visible",
    ...(isMdOrLarger.value
      ? [desktopSidebarClass.value]
      : mobileSidebarClass.value),
  ])

  function handleResize() {
    windowWidth.value = window.innerWidth
  }

  function closeSidebarOnMobile() {
    if (!isMdOrLarger.value) {
      sidebarOpened.value = false
    }
  }

  watch(
    () => [
      currentNotebookId.value,
      route.name === "noteShow" ? route.params.noteId : undefined,
    ],
    closeSidebarOnMobile
  )

  onMounted(() => {
    window.addEventListener("resize", handleResize)
    if (windowWidth.value >= SIDEBAR_BREAKPOINT_PX) {
      sidebarOpened.value = true
    }
  })

  onBeforeUnmount(() => {
    window.removeEventListener("resize", handleResize)
  })

  return { sidebarOpened, isMdOrLarger, sidebarClasses }
}
