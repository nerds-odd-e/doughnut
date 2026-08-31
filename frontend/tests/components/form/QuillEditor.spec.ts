import { mount, type VueWrapper } from "@vue/test-utils"
import QuillEditor from "@/components/form/QuillEditor.vue"
import { nextTick } from "vue"
import type Quill from "quill"
import { describe, it, expect, afterEach } from "vitest"
import routes from "@/routes/routes"
import { noteShowLocation } from "@/routes/noteShowLocation"
import { createRouter, createWebHistory } from "vue-router"

const router = createRouter({ history: createWebHistory(), routes })

describe("QuillEditor.vue", () => {
  let wrapper: VueWrapper

  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ""
  })

  async function mountEditor(
    props: Record<string, unknown> = { modelValue: "" }
  ) {
    wrapper = mount(QuillEditor, {
      props,
      attachTo: document.body,
      global: { plugins: [router] },
    })
    await nextTick()
    return wrapper
  }

  function quillInstance(): Quill {
    // biome-ignore lint/suspicious/noExplicitAny: Quill instance is not part of the public API
    const quill = (wrapper.vm as any).quill as Quill | null
    expect(quill).not.toBeNull()
    return quill!
  }

  async function clickEditorAnchor(selector: string) {
    await vi.waitUntil(() => document.querySelector(selector))
    const anchor = document.querySelector(selector) as HTMLAnchorElement
    anchor.dispatchEvent(
      new MouseEvent("click", { bubbles: true, cancelable: true })
    )
    await nextTick()
  }

  it("renders simple HTML content", async () => {
    await mountEditor({ modelValue: `<h1>Hello</h1><p>World</p>` })
    await vi.waitUntil(() => document.querySelector(".ql-editor h1"))
    expect(document.querySelector(".ql-editor h1")).toHaveTextContent("Hello")
    expect(document.querySelector(".ql-editor p")).toHaveTextContent("World")
  })

  it("preserves inline code from markdown HTML (<code>)", async () => {
    await mountEditor({
      modelValue: `<p>Use <code>foo</code> for this.</p>`,
    })
    await vi.waitUntil(() => document.querySelector(".ql-editor code"))
    expect(document.querySelector(".ql-editor code")).toHaveTextContent("foo")
  })

  it("emits softbreak HTML when pasting Hello<br>World", async () => {
    await mountEditor()
    await vi.waitUntil(() => document.querySelector(".ql-editor"))
    const editor = document.querySelector(".ql-editor") as HTMLElement
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

  it("passes preserve_pre: true when pasting HTML with code blocks", async () => {
    await mountEditor({ modelValue: "", readonly: false })
    const quill = quillInstance()
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

  it("opens http(s) links in a new window and in-app links via the router", async () => {
    const openSpy = vi.spyOn(window, "open").mockImplementation(() => null)
    const pushSpy = vi.spyOn(router, "push").mockResolvedValue(undefined)

    await mountEditor({
      modelValue:
        '<p><a href="https://example.com/path">ext</a> <a href="/n1" class="donut-wiki-link" data-note-id="1">wiki</a></p>',
      readonly: true,
    })
    await clickEditorAnchor(".ql-editor a[href='https://example.com/path']")
    expect(openSpy).toHaveBeenCalledWith(
      "https://example.com/path",
      "_blank",
      "noopener,noreferrer"
    )
    expect(pushSpy).not.toHaveBeenCalled()

    openSpy.mockClear()
    pushSpy.mockClear()

    await clickEditorAnchor(".ql-editor a.donut-wiki-link")
    expect(openSpy).not.toHaveBeenCalled()
    expect(pushSpy).toHaveBeenCalledWith(noteShowLocation(1))

    openSpy.mockRestore()
    pushSpy.mockRestore()
  })

  it.each([
    { case: "hash href", href: "#" },
    { case: "empty href", href: "" },
  ])(
    "emits deadWikiLinkClick when a dead wiki link with $case is clicked",
    async ({ href }) => {
      await mountEditor({
        modelValue: `<p><a href="${href}" class="dead-wiki-link" data-wiki-title="Ghost">Ghost</a></p>`,
        readonly: false,
      })
      await clickEditorAnchor(".ql-editor a.dead-wiki-link")

      expect(wrapper.emitted("deadWikiLinkClick")?.[0]).toEqual([
        { portablePath: "Ghost", displayText: "Ghost" },
      ])
    }
  )

  it("does not emit deadWikiLinkClick when a pending wiki link is clicked", async () => {
    const pushSpy = vi.spyOn(router, "push").mockResolvedValue(undefined)

    await mountEditor({
      modelValue: `<p><a href="#" class="pending-wiki-link" data-wiki-title="Ghost">Ghost</a></p>`,
      readonly: false,
    })
    await clickEditorAnchor(".ql-editor a.pending-wiki-link")

    expect(wrapper.emitted("deadWikiLinkClick")).toBeUndefined()
    expect(pushSpy).not.toHaveBeenCalled()

    pushSpy.mockRestore()
  })
})
