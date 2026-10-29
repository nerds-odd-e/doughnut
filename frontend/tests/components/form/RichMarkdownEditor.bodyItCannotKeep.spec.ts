import { flushPromises } from "@vue/test-utils"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

describe("RichMarkdownEditor with a body it cannot keep", () => {
  const h = createRichMarkdownEditorTestHarness()

  afterEach(() => {
    h.cleanup()
  })

  const warning = () =>
    h.getWrapper().find('[data-testid="rich-note-unavailable-warning"]')
  const warningText = () => warning().text()

  const convertButton = () =>
    warning()
      .findAll("button")
      .find((b) => b.text() === "Convert for the rich editor")!

  async function convert() {
    await convertButton().trigger("click")
    await flushPromises()
    await h.getWrapper().setProps({ modelValue: h.lastEmittedMarkdown() })
    await flushPromises()
  }

  it.each([
    "<details><summary>S</summary>Inner</details>",
    "- [ ] todo\n- [x] done",
    "line one  \nline two",
    "**a** *b*",
    "```ts\nconst a = 1\n```\n\n```\nplain\n```",
  ])("opens read-only with the warning: %j", async (body) => {
    await h.mountEditor(body)

    expect(h.quillReadonly()).toBe(true)
    expect(warningText()).toContain("rich editor cannot keep")
    expect(warningText()).toContain(
      "Converting keeps the text and removes formatting the rich editor cannot keep."
    )
    expect(convertButton().exists()).toBe(true)
  })

  it.each([
    ["<details><summary>S</summary>Inner</details>", "S\n\nInner"],
    ["text ~~strike~~ and more", "text strike and more"],
    ["line one  \nline two", "line one\n\nline two"],
    ["**a** *b*", "a b"],
    ["- [ ] todo\n- [x] done", "- todo\n- done"],
    [
      "# Kept heading\n\ntext ~~strike~~ and more\n\n- kept item",
      "# Kept heading\n\ntext strike and more\n\n- kept item",
    ],
    [
      "```ts\nconst a = 1\n```\n\n```\nplain\n```",
      "```\nconst a = 1\nplain\n```",
    ],
  ])(
    "converting %j keeps its text and makes it editable",
    async (body, converted) => {
      await h.mountEditor(body)

      await convert()

      expect(h.lastEmittedMarkdown()).toBe(converted)
      expect(h.getWrapper().emitted("blur")).toHaveLength(1)
      expect(warning().exists()).toBe(false)
      expect(h.quillReadonly()).toBe(false)
    }
  )

  it("converting keeps the frontmatter as written", async () => {
    await h.mountEditor("---\ntopic:   tea  # note\n---\n\n**a** *b*")

    await convert()

    expect(h.lastEmittedMarkdown()).toBe(
      "---\ntopic:   tea  # note\n---\n\na b"
    )
  })

  it("converts breaks beside a list into content that stays editable when reopened", async () => {
    await h.mountEditor("- <br> item\n- second\n\n<br>\n<br>\n例：\n\nB")

    await convert()

    expect(h.lastEmittedMarkdown()).toBe("- <br> item\n- second\n\n例：\n\nB")
    expect(h.quillReadonly()).toBe(false)
    expect(h.quillEditorEl().getAttribute("contenteditable")).toBe("true")

    const saved = h.lastEmittedMarkdown()
    h.cleanup()
    await h.mountEditor(saved)

    expect(h.quillReadonly()).toBe(false)
    expect(h.getWrapper().emitted("update:modelValue")).toBeUndefined()
  })

  it.each(["&nbsp;", "<p></p>", "<br/>\n"])(
    "opens a blank-looking body editable without changing it: %j",
    async (body) => {
      await h.mountEditor(body)

      expect(warning().exists()).toBe(false)
      expect(h.quillReadonly()).toBe(false)
      expect(h.getWrapper().emitted("update:modelValue")).toBeUndefined()
    }
  )

  it("saves only the typed text into a blank-looking body", async () => {
    await h.mountEditor("&nbsp;")

    h.quillInstance().insertText(0, "Hello", "user")
    expect(h.lastEmittedMarkdown()).toBe("Hello")
  })

  it.each([
    "# Heading\n\n- one\n- two\n\nSee [the site](https://example.com).",
    "Heading\n=======\n\n* one\n* two\n\nSee [the site][site].\n\n[site]: https://example.com",
    "See [[Some Note]] and [[Other|alias]].",
    "line one\nline two",
    "- item that wraps\n  onto a second line\n- two",
  ])("keeps a body with only style differences editable: %j", async (body) => {
    await h.mountEditor(body)

    expect(h.quillReadonly()).toBe(false)
    h.quillInstance().insertText(0, "Typed ", "user")
    expect(h.lastEmittedMarkdown()).toContain("Typed")
  })
})
