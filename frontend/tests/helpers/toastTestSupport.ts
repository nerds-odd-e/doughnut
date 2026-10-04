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
  document.querySelector<HTMLElement>(".Vue-Toastification__toast")

/** Waits for a toast of the given type and returns it. */
export async function toastShown(type: "error" | "warning") {
  await vi.waitFor(() => expect(toastOnPage()).not.toBeNull())
  const toast = toastOnPage()!
  expect(toast).toHaveClass(`Vue-Toastification__toast--${type}`)
  return toast
}

/** How long the toast stays, e.g. "3000ms", read from its progress bar. */
export const toastTimeout = (toast: HTMLElement) =>
  toast.querySelector<HTMLElement>(".Vue-Toastification__progress-bar")?.style
    .animationDuration

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
