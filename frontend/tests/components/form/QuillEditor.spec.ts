import { describe, it, expect, afterEach } from "vitest"
import { nextTick } from "vue"
import { noteShowLocation } from "@/routes/noteShowLocation"
import { createQuillEditorTestHarness } from "./quillEditorTestHarness"

describe("QuillEditor.vue", () => {
  const h = createQuillEditorTestHarness()

  afterEach(() => h.cleanup())

  it("renders simple HTML content", async () => {
    await h.mountEditor({ modelValue: `<h1>Hello</h1><p>World</p>` })
    await vi.waitUntil(() => document.querySelector(".ql-editor h1"))
    expect(document.querySelector(".ql-editor h1")).toHaveTextContent("Hello")
    expect(document.querySelector(".ql-editor p")).toHaveTextContent("World")
  })

  it("preserves inline code from markdown HTML (<code>)", async () => {
    await h.mountEditor({
      modelValue: `<p>Use <code>foo</code> for this.</p>`,
    })
    await vi.waitUntil(() => document.querySelector(".ql-editor code"))
    expect(document.querySelector(".ql-editor code")).toHaveTextContent("foo")
  })

  it("preserves Donut rich markup when the model changes", async () => {
    const wrapper = await h.mountEditor({ modelValue: "<p>Other note</p>" })

    await wrapper.setProps({
      modelValue:
        '<p><a href="/n1" class="donut-wiki-link" data-portable-path="First note" data-display-text="First" data-note-id="1">First</a><br class="softbreak"><mark>answer</mark></p><hr><table><tbody><tr><td>cell</td></tr></tbody></table>',
    })
    await nextTick()

    const wikiLink = document.querySelector(
      ".ql-editor a.donut-wiki-link"
    ) as HTMLAnchorElement
    expect(wikiLink).toHaveAttribute("href", "/n1")
    expect(wikiLink).toHaveAttribute("data-portable-path", "First note")
    expect(wikiLink).toHaveAttribute("data-display-text", "First")
    expect(wikiLink).toHaveAttribute("data-note-id", "1")
    expect(document.querySelector(".ql-editor br.softbreak")).not.toBeNull()
    expect(document.querySelector(".ql-editor mark")).toHaveTextContent(
      "answer"
    )
    expect(document.querySelector(".ql-editor hr")).not.toBeNull()
    expect(document.querySelector(".ql-editor table td")).toHaveTextContent(
      "cell"
    )
    expect(wrapper.emitted()["update:modelValue"]).toBeUndefined()
  })

  it("opens http(s) links in a new window and in-app links via the router", async () => {
    const openSpy = vi.spyOn(window, "open").mockImplementation(() => null)
    const pushSpy = vi.spyOn(h.router, "push").mockResolvedValue(undefined)

    await h.mountEditor({
      modelValue:
        '<p><a href="https://example.com/path">ext</a> <a href="/n1" class="donut-wiki-link" data-note-id="1">wiki</a></p>',
      readonly: true,
    })
    await h.clickEditorAnchor(".ql-editor a[href='https://example.com/path']")
    expect(openSpy).toHaveBeenCalledWith(
      "https://example.com/path",
      "_blank",
      "noopener,noreferrer"
    )
    expect(pushSpy).not.toHaveBeenCalled()

    openSpy.mockClear()
    pushSpy.mockClear()

    await h.clickEditorAnchor(".ql-editor a.donut-wiki-link")
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
      const wrapper = await h.mountEditor({
        modelValue: `<p><a href="${href}" class="dead-wiki-link" data-portable-path="Ghost">Ghost</a></p>`,
        readonly: false,
      })
      await h.clickEditorAnchor(".ql-editor a.dead-wiki-link")

      expect(wrapper.emitted("deadWikiLinkClick")?.[0]).toEqual([
        { portablePath: "Ghost", displayText: "Ghost" },
      ])
    }
  )

  it("does not emit deadWikiLinkClick when a pending wiki link is clicked", async () => {
    const pushSpy = vi.spyOn(h.router, "push").mockResolvedValue(undefined)

    const wrapper = await h.mountEditor({
      modelValue: `<p><a href="#" class="pending-wiki-link" data-portable-path="Ghost">Ghost</a></p>`,
      readonly: false,
    })
    await h.clickEditorAnchor(".ql-editor a.pending-wiki-link")

    expect(wrapper.emitted("deadWikiLinkClick")).toBeUndefined()
    expect(pushSpy).not.toHaveBeenCalled()

    pushSpy.mockRestore()
  })
})
