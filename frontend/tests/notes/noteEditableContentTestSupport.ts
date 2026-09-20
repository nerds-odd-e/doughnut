import {
  MemoryTrackerController,
  NoteController,
  TextContentController,
} from "@generated/donut-backend-api/sdk.gen"
import NoteEditableContent from "@/components/notes/core/NoteEditableContent.vue"
import type { UpdateNoteContentData } from "@generated/donut-backend-api"
import usePopups from "@/components/commons/Popups/usePopups"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import type { ComponentPublicInstance } from "vue"
import type Quill from "quill"
import type { Range as QuillRange } from "quill"
import makeMe from "donut-test-fixtures/makeMe"
import helper, { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import { vi } from "vitest"

export const markdownTextareaDefaults = {
  readonly: false,
  asMarkdown: true,
  wikiLinks: [] as string[],
}

export const trackedPropertyNoteId = 42
export const trackedPropertyMarkdown = `---
topic: training
---

Workshop body.`

export function mountNoteEditableContent(
  props: {
    noteId: number
    noteContent?: string
    readonly?: boolean
    asMarkdown?: boolean
    wikiLinks?: string[]
  },
  options?: { attachTo?: HTMLElement }
) {
  const chain = helper
    .component(NoteEditableContent)
    .withCleanStorage()
    .withRouter()
    .withProps({ ...markdownTextareaDefaults, ...props })
  return options?.attachTo
    ? chain.mount({ attachTo: options.attachTo })
    : chain.mount()
}

export async function mountMarkdownTextarea(props: {
  noteId: number
  noteContent: string
}) {
  const wrapper = mountNoteEditableContent(props)
  await flushPromises()
  return wrapper
}

export function textareaEl(wrapper: VueWrapper<ComponentPublicInstance>) {
  return wrapper.find("textarea").element as HTMLTextAreaElement
}

export async function setTextareaValue(
  wrapper: VueWrapper<ComponentPublicInstance>,
  value: string
) {
  const el = textareaEl(wrapper)
  el.value = value
  el.dispatchEvent(new Event("input"))
  await flushPromises()
  return el
}

export async function blurTextarea(
  wrapper: VueWrapper<ComponentPublicInstance>
) {
  await wrapper.find("textarea").trigger("blur")
  await flushPromises()
}

export function createClipboardEvent(
  html: string,
  plainText?: string
): ClipboardEvent {
  const event = new ClipboardEvent("paste", {
    bubbles: true,
    cancelable: true,
    clipboardData: new DataTransfer(),
  })
  event.clipboardData?.setData("text/html", html)
  if (plainText !== undefined) {
    event.clipboardData?.setData("text/plain", plainText)
  }
  return event
}

export function richQuillEditorEl(
  wrapper: VueWrapper<ComponentPublicInstance>
): HTMLElement {
  return wrapper.find(".ql-editor").element as HTMLElement
}

/** Clears any native DOM selection before a second Quill mutation in a test. Quill's own
 * `modify()` wrapper (run by every mutating Quill API) reads the current native selection to
 * restore it after the change; in this headless browser-mode environment, a *stale* native
 * range left over from an earlier real DOM mutation can no longer be resolved back to a blot
 * and throws. With zero ranges, Quill's read returns null and skips that restore step instead,
 * while the content mutation itself still applies normally - matching the plan's own recorded
 * limitation that only Quill's selection-read APIs are unreliable here, not Delta application. */
export function clearNativeSelectionForQuillMutation() {
  document.getSelection()?.removeAllRanges()
}

export function richQuillInstance(
  wrapper: VueWrapper<ComponentPublicInstance>
): Quill {
  const quillComponent = wrapper.findComponent({ name: "QuillEditor" })
  // biome-ignore lint/suspicious/noExplicitAny: Quill instance is not part of the public API
  return (quillComponent.vm as any).quill as Quill
}

/** Dispatches a real rich (Quill) paste `ClipboardEvent`, stubbing `getSelection` for the
 * given selection the way `QuillEditor.paste.spec.ts` does, since Quill's own selection-read
 * APIs are unreliable for a real native caret in this headless browser-mode test run. */
export async function dispatchRichPaste(
  wrapper: VueWrapper<ComponentPublicInstance>,
  html: string,
  options: { plainText?: string; selection: { index: number; length?: number } }
) {
  const editorEl = richQuillEditorEl(wrapper)
  editorEl.focus()
  const { index, length = 0 } = options.selection
  const getSelectionSpy = vi
    .spyOn(richQuillInstance(wrapper), "getSelection")
    .mockReturnValue({ index, length } as QuillRange)

  const clipboardData = new DataTransfer()
  clipboardData.setData("text/html", html)
  if (options.plainText !== undefined) {
    clipboardData.setData("text/plain", options.plainText)
  }
  editorEl.dispatchEvent(
    new ClipboardEvent("paste", {
      bubbles: true,
      cancelable: true,
      clipboardData,
    })
  )
  await flushPromises()
  // Only needed to make the capture above deterministic; a stale stub would
  // otherwise feed a fake range into Quill's own selection-restore machinery
  // on any later mutation in the same test.
  getSelectionSpy.mockRestore()
}

export function emitRichEditorPasteComplete(
  wrapper: VueWrapper<ComponentPublicInstance>,
  newContent: string
) {
  const richEditor = wrapper.findComponent({ name: "RichMarkdownEditor" })
  richEditor.vm.$emit("update:modelValue", newContent)
  richEditor.vm.$emit("pasteComplete", newContent)
}

export function setupUpdateNoteContentMock() {
  return mockSdkService(
    TextContentController,
    "updateNoteContent",
    makeMe.aNoteRealm.please()
  )
}

export function setupPopupsMock(
  // biome-ignore lint/suspicious/noExplicitAny: Mock type for testing
  mockPopupsOptions: any,
  overrides?: {
    confirm?: (msg: string) => Promise<boolean>
  }
) {
  vi.mocked(usePopups).mockReturnValue({
    popups: {
      options: mockPopupsOptions,
      alert: vi.fn(),
      confirm: overrides?.confirm ?? vi.fn(),
      done: vi.fn(),
      register: vi.fn(),
      peek: vi.fn(),
    },
  })
}

export function setupMemoryTrackerSdkMocks() {
  const getNoteInfoSpy = mockSdkService(NoteController, "getNoteInfo", {
    memoryTrackers: [],
  })
  const deleteSpy = mockSdkService(MemoryTrackerController, "delete", undefined)
  const updatePropertyKeySpy = mockSdkService(
    MemoryTrackerController,
    "updatePropertyKey",
    undefined
  )
  return { getNoteInfoSpy, deleteSpy, updatePropertyKeySpy }
}

export function mockNoteInfoWithPropertyTracker(
  getNoteInfoSpy: ReturnType<typeof mockSdkService>,
  key: string,
  id: number
) {
  const tracker = makeMe.aMemoryTracker.id(id).withPropertyKey(key).please()
  getNoteInfoSpy.mockResolvedValue(
    wrapSdkResponse(makeMe.aNoteRecallInfo.memoryTrackers([tracker]).please())
  )
  return tracker
}

export function mockDelayedFirstSave(
  updateNoteContentSpy: ReturnType<typeof mockSdkService>,
  noteId: number
) {
  let resolveFirstSave: (() => void) | undefined
  const firstSavePromise = new Promise<void>((resolve) => {
    resolveFirstSave = resolve
  })

  updateNoteContentSpy.mockImplementation((async (
    options: UpdateNoteContentData
  ) => {
    if (options.body?.content === "First edit") {
      await firstSavePromise
    }
    return wrapSdkResponse({
      id: noteId,
      note: {
        id: noteId,
        content: options.body?.content,
        noteTopology: { id: noteId, title: "Test Note" },
      },
    })
    // biome-ignore lint/suspicious/noExplicitAny: Vitest mock typing requires any for implementation functions
  }) as any)

  return () => {
    resolveFirstSave!()
  }
}
