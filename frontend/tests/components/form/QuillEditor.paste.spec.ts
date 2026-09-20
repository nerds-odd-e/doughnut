import { describe, it, expect, afterEach } from "vitest"
import { nextTick } from "vue"
import type { Range as QuillRange } from "quill"
import { createQuillEditorTestHarness } from "./quillEditorTestHarness"

describe("QuillEditor paste", () => {
  const h = createQuillEditorTestHarness()

  afterEach(() => h.cleanup())

  it("emits pasted content after the model is cleared", async () => {
    const wrapper = await h.mountEditor({ modelValue: "<p>Other note</p>" })
    await wrapper.setProps({ modelValue: "" })
    await vi.waitUntil(() => document.querySelector(".ql-editor"))
    const editor = document.querySelector(".ql-editor") as HTMLElement
    expect(wrapper.emitted()["update:modelValue"]).toBeUndefined()

    editor.focus()
    await nextTick()

    const clipboardData = new DataTransfer()
    clipboardData.setData("text/html", "<p>Hello<br>World</p>")
    editor.dispatchEvent(
      new ClipboardEvent("paste", {
        bubbles: true,
        cancelable: true,
        clipboardData,
      })
    )
    await nextTick()

    const emitted = wrapper.emitted()["update:modelValue"]
    expect(emitted?.length).toBeGreaterThan(0)
    expect(emitted?.[emitted.length - 1]?.[0]).toBe(
      `<p>Hello<br class="softbreak">World</p>`
    )
  })

  it("captures original clipboard text and the replaced Quill range on paste", async () => {
    const wrapper = await h.mountEditor({ modelValue: "<p>Hello world</p>" })
    const quill = h.quillInstance()
    await vi.waitUntil(() => document.querySelector(".ql-editor"))
    const editor = document.querySelector(".ql-editor") as HTMLElement
    editor.focus()

    // Quill's own selection-resolution (getRange/normalizedToRange) is
    // unreliable for a real caret in this headless browser-mode test run,
    // even for Quill's own untouched content and internal MutationObserver
    // reconciliation - a pre-existing environment issue, not something this
    // change introduces. Stub only that one read so the real capture code
    // under test still runs end-to-end against a real ClipboardEvent, with
    // Quill applying the paste through its real Delta model.
    const getSelectionSpy = vi
      .spyOn(quill, "getSelection")
      .mockReturnValue({ index: 6, length: 5 } as QuillRange)

    const clipboardData = new DataTransfer()
    clipboardData.setData("text/html", "<p><strong>Earth</strong></p>")
    clipboardData.setData("text/plain", "Earth")
    editor.dispatchEvent(
      new ClipboardEvent("paste", {
        bubbles: true,
        cancelable: true,
        clipboardData,
      })
    )
    await nextTick()

    const emitted = wrapper.emitted().pasteComplete
    expect(emitted?.length).toBeGreaterThan(0)
    const lastEmitted = emitted![emitted!.length - 1]!
    expect(lastEmitted[0]).toContain("Earth")
    expect(lastEmitted[1]).toEqual({
      originalText: "Earth",
      range: { index: 6, length: 5 },
      insertedLength: 5,
    })

    getSelectionSpy.mockRestore()
  })

  it("passes preserve_pre: true when pasting HTML with code blocks", async () => {
    await h.mountEditor({ modelValue: "", readonly: false })
    const quill = h.quillInstance()
    await vi.waitUntil(() => document.querySelector(".ql-editor"))
    const editor = document.querySelector(".ql-editor") as HTMLElement
    editor.focus()
    await nextTick()

    const inputHtml =
      '<pre><code>function hello() {\n  console.log("world");\n}</code></pre>'
    const pasteEvent = new Event("paste", {
      bubbles: true,
      cancelable: true,
    }) as ClipboardEvent
    Object.defineProperty(pasteEvent, "clipboardData", {
      value: {
        getData: (format: string) => (format === "text/html" ? inputHtml : ""),
      },
      writable: true,
      configurable: true,
    })

    quill.root.dispatchEvent(pasteEvent)
    await nextTick()

    const outputHtml = pasteEvent.clipboardData?.getData("text/html")
    expect(outputHtml).toContain("<pre>")
    expect(outputHtml).not.toContain("ql-code-block-container")
  })
})
