import SeamlessTextEditor from "@/components/form/SeamlessTextEditor.vue"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { nextTick } from "vue"
import helper from "@tests/helpers"

export const editorSelector = ".seamless-editor"

export function editorEl(wrapper: VueWrapper): HTMLElement {
  const root = wrapper.element as HTMLElement
  if (root.classList?.contains("seamless-editor")) {
    return root
  }
  const el = root.querySelector?.(editorSelector)
  if (el) return el as HTMLElement
  return wrapper.find(editorSelector).element as HTMLElement
}

export async function mountSeamlessTextEditor(
  initialValue: string,
  options: Record<string, unknown> = {},
  mountOpts?: { attachTo?: HTMLElement }
): Promise<VueWrapper> {
  const wrapper = helper
    .component(SeamlessTextEditor)
    .withProps({
      modelValue: initialValue,
      ...options,
    })
    .mount(mountOpts?.attachTo ? { attachTo: mountOpts.attachTo } : undefined)
  await flushPromises()
  return wrapper
}

export function setCaretInEditor(editor: HTMLElement, offset: number) {
  const textNode = editor.firstChild as Text
  const selection = window.getSelection()
  const range = document.createRange()
  range.setStart(textNode, offset)
  range.setEnd(textNode, offset)
  selection?.removeAllRanges()
  selection?.addRange(range)
}

export async function focusEditor(editor: HTMLElement) {
  editor.focus()
  await nextTick()
}

export async function pasteClipboard(
  editor: HTMLElement,
  options: { plain?: string; html?: string; clearSelection?: boolean }
) {
  if (options.clearSelection) {
    window.getSelection()?.removeAllRanges()
  }
  const clipboardData = new DataTransfer()
  if (options.plain !== undefined) {
    clipboardData.setData("text/plain", options.plain)
  }
  if (options.html) {
    clipboardData.setData("text/html", options.html)
  }
  editor.dispatchEvent(
    new ClipboardEvent("paste", {
      bubbles: true,
      cancelable: true,
      clipboardData,
    })
  )
  await nextTick()
  await flushPromises()
}

type PasteClipboardOptions = {
  plain?: string
  html?: string
  clearSelection?: boolean
}

type PasteSuccessCase = {
  case: string
  initialValue: string
  caretOffset?: number
  paste: PasteClipboardOptions
  expected?: string
  expectedContains?: readonly string[]
}

type PasteNoUpdateCase = {
  case: string
  initialValue: string
  props: Record<string, unknown>
  paste: PasteClipboardOptions
  expectedInnerText?: string
}

export const PASTE_NO_UPDATE_CASES: PasteNoUpdateCase[] = [
  {
    case: "readonly",
    initialValue: "",
    props: { readonly: true } as Record<string, unknown>,
    paste: { plain: "Test" },
  },
  {
    case: "empty clipboard",
    initialValue: "existing",
    props: {} as Record<string, unknown>,
    paste: {},
    expectedInnerText: "existing",
  },
]

export const PASTE_SUCCESS_CASES: PasteSuccessCase[] = [
  {
    case: "html content extracts plain text",
    initialValue: "",
    paste: {
      plain: "Bold text",
      html: "<p><strong>Bold text</strong></p>",
    },
    expected: "Bold text",
  },
  {
    case: "at cursor position",
    initialValue: "existing text",
    caretOffset: 8,
    paste: { plain: " inserted" },
    expectedContains: ["existing", "inserted", "text"] as const,
  },
  {
    case: "append when no selection",
    initialValue: "existing",
    paste: { plain: " appended", clearSelection: true },
    expected: "existing appended",
  },
]
