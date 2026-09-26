import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import RichMarkdownEditor from "@/components/form/RichMarkdownEditor.vue"
import NoteToolbar from "@/components/notes/core/NoteToolbar.vue"
import { teardownGlobalClientForTesting } from "@/managedApi/clientSetup"
import { wikiLinkFromAuthoredToken } from "@/utils/wikiLinkMarkup"
import helper, { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import GlobalApiLoadingModal from "@tests/helpers/GlobalApiLoadingModal"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import { defineComponent, nextTick, ref } from "vue"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

describe("RichMarkdownEditor", () => {
  const h = createRichMarkdownEditorTestHarness()

  afterEach(() => {
    h.cleanup()
  })

  it("does not emit update:modelValue on mount", async () => {
    await h.mountEditor("initial value")
    expect(h.getWrapper().emitted()["update:modelValue"]).toBeUndefined()
    await h.mountEditor("# Title", { readonly: true })
    expect(h.getWrapper().emitted()["update:modelValue"]).toBeUndefined()
  })

  it("prompts for readme content when empty in readme context", async () => {
    await h.mountEditor("", { isReadmeContext: true, attachToBody: true })
    expect(h.quillEditorEl().getAttribute("data-placeholder")).toBe(
      "Enter readme content here..."
    )
  })

  it("converts pasted HTML to markdown", async () => {
    await h.mountEditor("", { attachToBody: true })
    await h.dispatchPasteHtmlToQuill("<p><strong>Bold text</strong></p>")
    expect(h.lastEmittedMarkdown()).toContain("Bold text")
  })

  it("threads the original clipboard text and Quill paste range to pasteComplete", async () => {
    await h.mountEditor("Hello world", { attachToBody: true })
    await h.dispatchPasteHtmlToQuill("<p><strong>Earth</strong></p>", {
      plainText: "Earth",
      selection: { index: 6, length: 5 },
    })

    expect(h.lastEmittedPasteContext()).toEqual({
      originalText: "Earth",
      range: { index: 6, length: 5 },
      insertedLength: 5,
    })
  })

  it("preserves nested bullet indentation when pasting ChatGPT-style HTML", async () => {
    await h.mountEditor("", { attachToBody: true })
    await h.dispatchPasteHtmlToQuill(
      `<p class="p1">Intro</p><ul><li><span class="s1"><b>Japan</b></span><ul><li>correct: circle</li><li>incorrect: cross</li></ul></li></ul>`
    )
    const markdown = h.lastEmittedMarkdown()
    expect(markdown).toMatch(/\n {2,}\* +correct: circle/)
    expect(markdown).toMatch(/\n {2,}\* +incorrect: cross/)
    expect(
      Array.from(h.quillEditorEl().querySelectorAll("li")).map((li) => ({
        text: li.textContent?.trim(),
        indent: li.className.match(/ql-indent-(\d+)/)?.[1] ?? "0",
      }))
    ).toEqual([
      { text: "Japan", indent: "0" },
      { text: "correct: circle", indent: "1" },
      { text: "incorrect: cross", indent: "1" },
    ])
  })

  it("keeps a body image embed when other text is edited", async () => {
    await h.mountEditor("Intro\n\n![force](force-diagram.png)\n\nMore", {
      attachToBody: true,
    })
    const quill = h.quillInstance()
    quill.insertText(quill.getLength() - 1, " text", "user")
    await nextTick()
    expect(h.lastEmittedMarkdown()).toBe(
      "Intro\n\n![force](force-diagram.png)\n\nMore text"
    )
  })

  it("does not paste when readonly", async () => {
    await h.mountEditor("", { readonly: true })
    await h.dispatchPasteHtmlToQuill("<p>Test</p>")
    expect(h.getWrapper().emitted()["update:modelValue"]).toBeUndefined()
  })

  it("linkifies wikilinks in Quill HTML while model matches the interval", async () => {
    const wikiLinks = [wikiLinkFromAuthoredToken("MyNote", 42)]
    const wrapper = await h.mountEditor("", { wikiLinks })
    h.emitQuillModelValue("<p>[[MyNote]]</p>")
    await wrapper.setProps({ modelValue: "[[MyNote]]" })
    await nextTick()

    expect(h.quillModelHtml()).toContain('class="donut-wiki-link"')
    expect(h.quillModelHtml()).toContain('data-portable-path="MyNote"')
  })

  it("keeps canonical dead-wiki-link HTML identical to Quill internal HTML", async () => {
    await h.mountEditor("[[Missing Note]]")
    const translatedHtml = h.quillModelHtml()

    expect(translatedHtml).toContain('class="dead-wiki-link"')
    expect(h.quillEditorEl().innerHTML).toBe(translatedHtml)
  })

  it("keeps active Quill HTML identical when saved markdown with a dead link is echoed back", async () => {
    const wrapper = await h.mountEditor("Hello")
    h.emitQuillModelValue("<p>Hello [[Missing Note]]</p>")
    await wrapper.setProps({ modelValue: "Hello [[Missing Note]]" })
    await nextTick()

    expect(h.quillModelHtml()).toContain('class="dead-wiki-link"')
    expect(h.quillModelHtml()).toBe(h.quillEditorEl().innerHTML)
  })

  describe("switching to Markdown mode", () => {
    let wrapper: VueWrapper

    afterEach(() => {
      wrapper?.unmount()
      teardownGlobalClientForTesting()
    })

    it("keeps the rich editor mounted while a property rename is checked, then shows the renamed source", async () => {
      let finishGuard!: () => void
      const guard = new Promise<void>((resolve) => {
        finishGuard = resolve
      })
      const noteInfo = mockSdkService(
        NoteController,
        "getNoteInfo",
        makeMe.aNoteRecallInfo.please()
      )
      noteInfo.mockImplementation(async () => {
        await guard
        return wrapSdkResponse(makeMe.aNoteRecallInfo.please())
      })
      const noteRealm = makeMe.aNoteRealm
        .content("---\ntopic: wiki\n---\n\nBody.")
        .please()
      const Harness = defineComponent({
        components: { RichMarkdownEditor, NoteToolbar, GlobalApiLoadingModal },
        setup: () => ({
          noteRealm,
          markdown: ref(noteRealm.note.content),
          asMarkdown: ref(false),
        }),
        template: `
          <GlobalApiLoadingModal />
          <NoteToolbar :note="noteRealm.note" :notebook-id="noteRealm.notebookRealm.notebook.id"
            :as-markdown="asMarkdown" @edit-as-markdown="asMarkdown = $event" />
          <textarea v-if="asMarkdown" v-model="markdown" />
          <RichMarkdownEditor v-else v-model="markdown" :note-id="noteRealm.note.id" :wiki-links="[]" />
        `,
      })
      wrapper = helper
        .component(Harness)
        .withCleanStorage()
        .withRouter()
        .mount({ attachTo: document.body })
      await flushPromises()
      const key = wrapper.find(
        '[data-testid="rich-note-property-row-key-input"]'
      )
      await key.trigger("focus")
      await key.setValue("domain")
      await key.trigger("blur")
      await flushPromises()
      expect(noteInfo).toHaveBeenCalled()
      const toggleMode = () =>
        document.dispatchEvent(
          new KeyboardEvent("keydown", {
            key: "m",
            code: "KeyM",
            bubbles: true,
            cancelable: true,
          })
        )
      toggleMode()
      await flushPromises()
      expect(wrapper.find("textarea").exists()).toBe(false)
      expect(document.querySelector("dialog:modal")).not.toBeNull()
      finishGuard()
      await flushPromises()
      expect(document.querySelector("dialog:modal")).toBeNull()
      ;(document.activeElement as HTMLElement)?.blur()
      toggleMode()
      await flushPromises()
      expect(wrapper.find("textarea").element).toHaveValue(
        "---\ndomain: wiki\n---\n\nBody."
      )
    })
  })
})
