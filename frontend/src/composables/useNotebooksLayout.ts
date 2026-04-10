import { onMounted, ref, watch } from "vue"

const NOTEBOOKS_LAYOUT_STORAGE_KEY = "doughnut.notebooksPage.layout"

export function useNotebooksLayout() {
  const notebooksLayout = ref<"list" | "grid">("list")

  onMounted(() => {
    const stored = localStorage.getItem(NOTEBOOKS_LAYOUT_STORAGE_KEY)
    if (stored === "list" || stored === "grid") {
      notebooksLayout.value = stored
    }
  })

  watch(notebooksLayout, (value) => {
    localStorage.setItem(NOTEBOOKS_LAYOUT_STORAGE_KEY, value)
  })

  return { notebooksLayout }
}
