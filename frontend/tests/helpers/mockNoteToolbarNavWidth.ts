import {
  noteMoreOptionsTitles,
  noteToolbarOverflowTitles,
  type NoteMoreOptionsActionId,
} from "@/components/notes/widgets/noteMoreOptionsTitles"
import { NOTE_TOOLBAR_MORE_OPTIONS_ORDER } from "@/composables/noteToolbarOverflow"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { vi } from "vitest"

type ResizeObserverCallback = () => void

const resizeObserverCallbacks: ResizeObserverCallback[] = []

export const noteToolbarActionWidth = 40
export const noteToolbarOverflowButtonWidth = 32
export const allMoreOptionsWidth =
  noteToolbarActionWidth * NOTE_TOOLBAR_MORE_OPTIONS_ORDER.length

type NoteToolbarOffsetWidthLayout = {
  actionWidths: Partial<Record<NoteMoreOptionsActionId, number>>
  overflowWidth: number
  precedingWidth: number
}

let offsetWidthLayout: NoteToolbarOffsetWidthLayout | undefined
let restoreOffsetWidth: (() => void) | undefined

const originalOffsetWidth = Object.getOwnPropertyDescriptor(
  HTMLElement.prototype,
  "offsetWidth"
)

const moreOptionTitleToId = new Map(
  NOTE_TOOLBAR_MORE_OPTIONS_ORDER.flatMap((id) =>
    noteToolbarOverflowTitles(id).map((title) => [title, id] as const)
  )
)

function mockedOffsetWidth(el: HTMLElement): number | undefined {
  if (!offsetWidthLayout) return
  const title = el.getAttribute("title")
  if (title === noteMoreOptionsTitles.overflowMenu) {
    return offsetWidthLayout.overflowWidth
  }
  if (title && moreOptionTitleToId.has(title)) {
    const id = moreOptionTitleToId.get(title)
    if (id === undefined) return noteToolbarActionWidth
    return offsetWidthLayout.actionWidths[id] ?? noteToolbarActionWidth
  }
  if (!el.closest("[data-note-toolbar]")) return
  if (el.hasAttribute("data-note-toolbar")) return
  return offsetWidthLayout.precedingWidth
}

function installOffsetWidthMock() {
  if (!originalOffsetWidth?.get || restoreOffsetWidth) return
  Object.defineProperty(HTMLElement.prototype, "offsetWidth", {
    configurable: true,
    get() {
      const mocked = mockedOffsetWidth(this as HTMLElement)
      if (mocked !== undefined) return mocked
      return originalOffsetWidth.get?.call(this) ?? 0
    },
  })
  restoreOffsetWidth = () => {
    Object.defineProperty(
      HTMLElement.prototype,
      "offsetWidth",
      originalOffsetWidth
    )
    restoreOffsetWidth = undefined
    offsetWidthLayout = undefined
  }
}

export function installMockResizeObserver() {
  resizeObserverCallbacks.length = 0
  offsetWidthLayout = undefined
  installOffsetWidthMock()
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

export function restoreNoteToolbarWidthMocks() {
  restoreOffsetWidth?.()
}

export function flushMockResizeObserver() {
  for (const callback of [...resizeObserverCallbacks]) {
    callback()
  }
}

function setNoteToolbarNavWidth(wrapper: VueWrapper, width: number) {
  const nav = wrapper.find("[data-note-toolbar]").element as HTMLElement
  Object.defineProperty(nav, "clientWidth", {
    configurable: true,
    get: () => width,
  })
  flushMockResizeObserver()
}

export function setNoteToolbarMeasuredLayout(
  wrapper: VueWrapper,
  options: {
    navWidth: number
    actionWidths?: Partial<Record<NoteMoreOptionsActionId, number>>
    overflowWidth?: number
    precedingWidth?: number
  }
) {
  offsetWidthLayout = {
    actionWidths: options.actionWidths ?? {},
    overflowWidth: options.overflowWidth ?? noteToolbarOverflowButtonWidth,
    precedingWidth: options.precedingWidth ?? 0,
  }
  setNoteToolbarNavWidth(wrapper, options.navWidth)
}

export async function layoutNoteToolbar(wrapper: VueWrapper, navWidth: number) {
  setNoteToolbarMeasuredLayout(wrapper, { navWidth })
  await flushPromises()
}

/** All overflowable actions including Edit fit; overflow button is not needed. */
export function allMoreOptionsFitNavWidth(precedingWidth = 0) {
  return precedingWidth + allMoreOptionsWidth
}

/** Export and the rest of more-options overflow; Edit still fits beside `…`. */
export function exportOverflowNavWidth(precedingWidth = 0) {
  return (
    precedingWidth + noteToolbarActionWidth + noteToolbarOverflowButtonWidth
  )
}

/** After Export is omitted, further shrinkage yields Edit into more options. */
export function editOverflowNavWidth(precedingWidth = 0) {
  return exportOverflowNavWidth(precedingWidth) - 1
}

/** Full set does not fit; remaining actions plus overflow button do. */
export function deleteOverflowNavWidth(precedingWidth = 0) {
  return precedingWidth + allMoreOptionsWidth - 1
}

/** Off-state audio and assimilation overflow (pin tests). */
export function overflowTogglesNavWidth(precedingWidth = 0) {
  return (
    precedingWidth + noteToolbarActionWidth * 2 + noteToolbarOverflowButtonWidth
  )
}
