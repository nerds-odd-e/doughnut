import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import { expandPropertyPanel, propertyRowSelector } from "./propertiesTestDom"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

const markdown = `---
example of:
  - "[[必死]]"
  - "[[そして]]"
---

Body`
const rowSelector = propertyRowSelector("example of")
const panelSelector = `${rowSelector} [data-testid="rich-note-property-panel"]`

describe("Property tracker loading", () => {
  const h = createRichMarkdownEditorTestHarness()
  afterEach(() => {
    h.cleanup()
    vi.restoreAllMocks()
  })

  it.each([320, 768])(
    "keeps list rows steady while tracker information loads at %ipx",
    async (width) => {
      let resolveInfo!: (value: ReturnType<typeof wrapSdkResponse>) => void
      mockSdkService(
        NoteController,
        "getNoteInfo",
        makeMe.aNoteRecallInfo.please()
      ).mockImplementation(
        () =>
          new Promise((resolve) => {
            resolveInfo = resolve
          })
      )
      const wrapper = await h.mountEditor(markdown, {
        noteId: 42,
        attachToBody: true,
      })
      ;(wrapper.element as HTMLElement).style.width = `${width}px`
      await expandPropertyPanel(wrapper, rowSelector)
      const panel = wrapper.get(panelSelector)
      expect(
        panel.findAll('[aria-label="Loading tracker information"]')
      ).toHaveLength(2)
      const rows = panel.findAll("[data-property-value]")
      const before = rows.map(
        (row) => row.element.getBoundingClientRect().height
      )
      const panelHeight = panel.element.getBoundingClientRect().height
      resolveInfo(
        wrapSdkResponse(
          makeMe.aNoteRecallInfo
            .memoryTrackers([
              makeMe.aMemoryTracker
                .id(77)
                .withPropertyKey("example of", "[[必死]]")
                .please(),
            ])
            .please()
        )
      )
      await flushPromises()
      expect(
        panel.find('[data-test="assimilation-status-UNDERSTANDING"]').text()
      ).toBe("View tracker")
      expect(panel.find('[aria-label="Assimilate [[そして]]"]').text()).toBe(
        "Assimilate"
      )
      expect(
        rows.map((row) => row.element.getBoundingClientRect().height)
      ).toEqual(before)
      expect(panel.element.getBoundingClientRect().height).toBe(panelHeight)
      const label = rows[0]!.get(
        '[data-test="mode-label-UNDERSTANDING"]'
      ).element
      expect(label.getBoundingClientRect().height).toBeLessThanOrEqual(24)
    }
  )

  it("loads tracker information for the current note when the editor changes notes", async () => {
    const getInfo = mockSdkService(
      NoteController,
      "getNoteInfo",
      makeMe.aNoteRecallInfo.please()
    )
    const wrapper = await h.mountEditor(markdown, { noteId: 42 })
    await expandPropertyPanel(wrapper, rowSelector)
    let finishLoading!: () => void
    getInfo.mockImplementation(async () => {
      await new Promise<void>((resolve) => {
        finishLoading = resolve
      })
      return wrapSdkResponse(makeMe.aNoteRecallInfo.please())
    })
    await wrapper.setProps({ noteId: 43 })
    expect(
      wrapper.findAll('[aria-label="Loading tracker information"]')
    ).toHaveLength(2)
    finishLoading()
    await flushPromises()
    expect(wrapper.find('[aria-label="Assimilate [[必死]]"]').text()).toBe(
      "Assimilate"
    )
  })
})
