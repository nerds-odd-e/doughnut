import { createApp, nextTick } from "vue"
import Toast, { useToast } from "vue-toastification"
import { beforeEach, expect, vi } from "vitest"

let toastContainerMounted: Promise<void> | undefined

// Each install of the plugin mounts another container on the page, one tick
// later, all listening to the same global event bus. So the container is
// mounted once per spec file, and a toast shown before it is mounted is lost.
function mountToastContainerOnce() {
  toastContainerMounted ??= (async () => {
    createApp({ render: () => null }).use(Toast)
    await nextTick()
  })()
  return toastContainerMounted
}

export const toastOnPage = () =>
  document.querySelector(".Vue-Toastification__toast")

/**
 * Shows the real toasts on the page: production code's `useToast` reaches one
 * toast container, emptied before each test so earlier toasts do not leak.
 */
export function showToastsOnPage() {
  beforeEach(async () => {
    await mountToastContainerOnce()
    useToast().clear()
    await vi.waitFor(() => expect(toastOnPage()).toBeNull())
  })
}
