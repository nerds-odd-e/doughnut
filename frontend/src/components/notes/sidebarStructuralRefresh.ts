import { ref } from "vue"

export const sidebarStructuralRefreshKey = ref(0)

export function refreshSidebarStructuralListings() {
  sidebarStructuralRefreshKey.value += 1
}
