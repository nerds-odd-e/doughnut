import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

describe("RichMarkdownEditor changes only what the user edited", () => {
  const h = createRichMarkdownEditorTestHarness()

  afterEach(() => {
    h.cleanup()
  })

  const typeAtStart = async (body: string, word: string) => {
    await h.mountEditor(body)
    h.quillInstance().insertText(0, word, "user")
    return h.lastEmittedMarkdown()
  }

  it("keeps common Markdown forms, changing only the edited line", async () => {
    const body = [
      "## Part",
      "",
      "- item",
      "- other",
      "",
      "Some *em* and **strong** text.",
      "",
      "---",
      "",
      "End.",
    ].join("\n")

    expect(await typeAtStart(body, "My ")).toBe(
      body.replace("## Part", "## My Part")
    )
  })

  it("normalizes other forms to the common ones", async () => {
    const body = ["Intro", "", "+ item", "+ other", "", "Some _em_ text."].join(
      "\n"
    )

    expect(await typeAtStart(body, "An ")).toBe(
      ["An Intro", "", "- item", "- other", "", "Some *em* text."].join("\n")
    )
  })
})
