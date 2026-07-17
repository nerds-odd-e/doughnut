import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { nextTick } from "vue"
import {
  editorEl,
  focusEditor,
  mountSeamlessTextEditor,
  PASTE_NO_UPDATE_CASES,
  PASTE_SUCCESS_CASES,
  pasteClipboard,
  setCaretInEditor,
} from "./seamlessTextEditorTestSupport"

describe("SeamlessTextEditor", () => {
  let wrapper: VueWrapper

  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ""
  })

  it("does not emit update when the change is from initial value", async () => {
    wrapper = await mountSeamlessTextEditor("initial value")
    expect(wrapper.emitted()["update:modelValue"]).toBeUndefined()
  })

  it("keeps caret offset when modelValue is synced with same-length text", async () => {
    wrapper = await mountSeamlessTextEditor(
      "x:y",
      {},
      { attachTo: document.body }
    )
    const editor = editorEl(wrapper)
    editor.focus()
    await nextTick()
    const textNode = editor.firstChild as Text
    const selection = window.getSelection()
    const range = document.createRange()
    range.setStart(textNode, 1)
    range.setEnd(textNode, 1)
    selection?.removeAllRanges()
    selection?.addRange(range)

    await wrapper.setProps({ modelValue: "x：y" })
    await flushPromises()
    await nextTick()

    const sel = window.getSelection()
    expect(sel?.rangeCount).toBe(1)
    const r = sel?.getRangeAt(0)
    expect(r?.startOffset).toBe(1)
    expect(r?.startContainer).toBe(editor.firstChild)
  })

  it.each(PASTE_SUCCESS_CASES)(
    "pastes plain text ($case)",
    async ({
      initialValue,
      caretOffset,
      paste,
      expected,
      expectedContains,
    }) => {
      wrapper = await mountSeamlessTextEditor(initialValue)
      const editor = editorEl(wrapper)
      await focusEditor(editor)
      if (caretOffset !== undefined) {
        setCaretInEditor(editor, caretOffset)
      }
      await pasteClipboard(editor, paste)

      const emitted = wrapper.emitted()["update:modelValue"]
      expect(emitted).toBeDefined()
      expect(emitted?.length).toBeGreaterThan(0)
      const finalText = emitted?.[emitted.length - 1]?.[0] as string
      if (expected !== undefined) {
        expect(finalText).toBe(expected)
        expect(editor.innerText).toBe(expected)
      } else {
        for (const part of expectedContains ?? []) {
          expect(finalText).toContain(part)
          expect(editor.innerText).toContain(part)
        }
      }
    }
  )

  it("submits the nearest form on Enter", async () => {
    const onSubmit = vi.fn((e: Event) => e.preventDefault())
    const form = document.createElement("form")
    form.addEventListener("submit", onSubmit)
    const submit = document.createElement("input")
    submit.type = "submit"
    form.appendChild(submit)

    wrapper = await mountSeamlessTextEditor("x", {}, { attachTo: form })
    document.body.appendChild(form)

    const editor = editorEl(wrapper)
    await focusEditor(editor)
    editor.dispatchEvent(
      new KeyboardEvent("keydown", {
        key: "Enter",
        bubbles: true,
        cancelable: true,
      })
    )
    await flushPromises()
    await nextTick()

    expect(onSubmit).toHaveBeenCalledTimes(1)
    form.remove()
  })

  it.each(PASTE_NO_UPDATE_CASES)(
    "does not handle paste when $case",
    async ({ initialValue, props, paste, expectedInnerText }) => {
      wrapper = await mountSeamlessTextEditor(initialValue, props)
      const editor = editorEl(wrapper)
      await focusEditor(editor)
      await pasteClipboard(editor, paste)

      expect(wrapper.emitted()["update:modelValue"]).toBeUndefined()
      if (expectedInnerText !== undefined) {
        expect(editor.innerText).toBe(expectedInnerText)
      }
    }
  )
})
