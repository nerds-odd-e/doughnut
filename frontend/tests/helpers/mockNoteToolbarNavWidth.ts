import { NOTE_TOOLBAR_MORE_OPTIONS_INLINE_MIN_PX } from "@/composables/useNoteToolbarMoreOptionsInline"
import type { VueWrapper } from "@vue/test-utils"
import { vi } from "vitest"

type ResizeObserverCallback = () => void

const resizeObserverCallbacks: ResizeObserverCallback[] = []

export function installMockResizeObserver() {
  resizeObserverCallbacks.length = 0
  vi.stubGlobal(
    "ResizeObserver",
    class MockResizeObserver {
      private readonly callback: ResizeObserverCallback

      constructor(callback: ResizeObserverCallback) {
        this.callback = callback
      }

      observe() {
        resizeObserverCallbacks.push(this.callback)
        this.callback()
      }

      disconnect() {
        const index = resizeObserverCallbacks.indexOf(this.callback)
        if (index >= 0) {
          resizeObserverCallbacks.splice(index, 1)
        }
      }
    }
  )
}

export function flushMockResizeObserver() {
  for (const callback of [...resizeObserverCallbacks]) {
    callback()
  }
}

export function setNoteToolbarNavWidth(wrapper: VueWrapper, width: number) {
  const nav = wrapper.find("[data-note-toolbar]").element as HTMLElement
  Object.defineProperty(nav, "clientWidth", {
    configurable: true,
    get: () => width,
  })
  flushMockResizeObserver()
}

export const narrowNoteToolbarNavWidth =
  NOTE_TOOLBAR_MORE_OPTIONS_INLINE_MIN_PX - 1

export const wideNoteToolbarNavWidth = NOTE_TOOLBAR_MORE_OPTIONS_INLINE_MIN_PX
