import { TextContentController } from "@generated/donut-backend-api/sdk.gen"
import NoteEditableContent from "@/components/notes/core/NoteEditableContent.vue"
import usePopups from "@/components/commons/Popups/usePopups"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import type { ComponentPublicInstance } from "vue"
import type Quill from "quill"
import type { Range as QuillRange } from "quill"
import makeMe from "donut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import { vi } from "vitest"

/** Mounts an editable note (Markdown mode unless overridden) and lets it settle. */
export async function mountNoteEditableContent(
  props: {
    noteId: number
    noteContent?: string
    readonly?: boolean
    asMarkdown?: boolean
    wikiLinks?: string[]
  },
  options: { attachTo?: HTMLElement } = {}
) {
  const wrapper = helper
    .component(NoteEditableContent)
    .withCleanStorage()
    .withRouter()
    .withProps({ readonly: false, asMarkdown: true, wikiLinks: [], ...props })
    .mount(options)
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

/** Dispatches a real textarea paste of `html` (and optional `text/plain`). */
export async function pasteIntoTextarea(
  textarea: HTMLTextAreaElement,
  html: string,
  plainText?: string
) {
  const clipboardData = new DataTransfer()
  clipboardData.setData("text/html", html)
  if (plainText !== undefined) clipboardData.setData("text/plain", plainText)
  textarea.dispatchEvent(
    new ClipboardEvent("paste", {
      bubbles: true,
      cancelable: true,
      clipboardData,
    })
  )
  await flushPromises()
}

/** Mounts note 1 in Markdown mode, selects `selection` (default: the end),
 * and pastes into its textarea. */
export async function mountAndPaste(
  noteContent: string,
  html: string,
  options: { plainText?: string; selection?: [number, number] } = {}
) {
  const wrapper = await mountNoteEditableContent(
    { noteId: 1, noteContent },
    { attachTo: document.body }
  )
  const textarea = textareaEl(wrapper)
  if (options.selection) textarea.setSelectionRange(...options.selection)
  await pasteIntoTextarea(textarea, html, options.plainText)
  return { wrapper, textarea }
}

export function choiceShown(wrapper: VueWrapper<ComponentPublicInstance>) {
  return wrapper.find('[data-testid="paste-choice"]').exists()
}

/** Applies the pending paste choice's "Use original text" action. */
export async function useOriginalText(
  wrapper: VueWrapper<ComponentPublicInstance>
) {
  await wrapper.find('[data-testid="paste-choice-action"]').trigger("click")
  await flushPromises()
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
