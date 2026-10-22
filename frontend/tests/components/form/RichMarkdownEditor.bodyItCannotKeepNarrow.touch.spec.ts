import { atViewport, rect } from "./propertyLayoutTestSupport"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

describe("RichMarkdownEditor body warning on a phone-width touch screen", () => {
  const h = createRichMarkdownEditorTestHarness()

  afterEach(() => {
    h.cleanup()
  })

  it("stacks message, consequence and a touch-sized button within the window", async () => {
    await atViewport(320)
    const wrapper = await h.mountEditor("**a** *b*", { attachToBody: true })

    const warning = wrapper.get('[data-testid="rich-note-unavailable-warning"]')
    const part = (text: string) =>
      warning.findAll("span, button").find((el) => el.text().startsWith(text))!
        .element
    const buttonEl = part("Convert for the rich editor") as HTMLElement
    const message = rect(part("This note has content"))
    const consequence = rect(part("Converting keeps the text"))
    const button = rect(buttonEl)

    for (const r of [message, consequence, button]) {
      expect(r.left).toBeGreaterThanOrEqual(0)
      expect(r.right).toBeLessThanOrEqual(window.innerWidth)
    }
    expect(message.bottom).toBeLessThanOrEqual(consequence.top)
    expect(consequence.bottom).toBeLessThanOrEqual(button.top)
    expect(buttonEl.scrollWidth).toBeLessThanOrEqual(buttonEl.clientWidth)
    expect(button.height).toBeGreaterThanOrEqual(44)
  })
})
