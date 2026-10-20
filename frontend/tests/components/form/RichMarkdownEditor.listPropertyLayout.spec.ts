import { notePropertyLocation } from "@/routes/noteShowLocation"
import {
  AssimilationController,
  NoteController,
} from "@generated/donut-backend-api/sdk.gen"
import { mockSdkService } from "@tests/helpers"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import {
  collapsePropertyPanel,
  expandPropertyPanel,
  propertyRowSelector,
} from "./propertiesTestDom"
import {
  contentRect,
  expectApart,
  expectInside,
  expectLegibleWholeValueInRow,
  expectWholeTextVisibleInRow,
  listItemLabels,
  listKey,
  mountAtWidth,
  rect,
  row,
  unbroken,
  widths,
} from "./propertyLayoutTestSupport"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

const panelSelector = `${propertyRowSelector(listKey)} [data-testid="rich-note-property-panel"]`

function detailedRows(wrapper: VueWrapper) {
  return wrapper
    .findAll(`${panelSelector} [data-property-value]`)
    .map((r) => r.element)
}

describe("RichMarkdownEditor list property layout", () => {
  const h = createRichMarkdownEditorTestHarness()

  beforeEach(() => {
    mockSdkService(
      NoteController,
      "getNoteInfo",
      makeMe.aNoteRecallInfo
        .memoryTrackers([
          makeMe.aMemoryTracker
            .id(77)
            .withPropertyKey(listKey, "practice")
            .please(),
        ])
        .please()
    )
  })

  afterEach(() => {
    vi.restoreAllMocks()
    h.cleanup()
  })

  async function mountExpandedListAtWidth(width: number) {
    const wrapper = await mountAtWidth(h, width)
    await expandPropertyPanel(wrapper, propertyRowSelector(listKey))
    return wrapper
  }

  describe.each(widths)("at %ipx", (width) => {
    it("shows each expanded list item whole, in authored order, apart from its learning control", async () => {
      const wrapper = await mountExpandedListAtWidth(width)
      const panel = wrapper.get(panelSelector).element
      expectInside(rect(panel), rect(wrapper.element))

      const rows = detailedRows(wrapper)
      expect(
        rows.map((r) =>
          r
            .querySelector('[data-test="mode-label-UNDERSTANDING"]')!
            .textContent!.trim()
        )
      ).toEqual(listItemLabels)
      for (const r of rows) {
        const label = r.querySelector(
          '[data-testid="rich-note-property-row-list-value"]'
        )!
        const action = r.querySelector(
          '[data-test="assimilation-action-UNDERSTANDING"] :is(a, button)'
        )
        expectWholeTextVisibleInRow(r, label, panel)
        if (action) {
          expectInside(rect(action), rect(panel))
          expectApart(contentRect(label), rect(action))
        }
      }
    })

    it("restores the whole compact list and its Edit control after collapsing", async () => {
      const wrapper = await mountExpandedListAtWidth(width)
      await collapsePropertyPanel(wrapper, propertyRowSelector(listKey))
      const r = row(wrapper, listKey)

      expectLegibleWholeValueInRow(
        r,
        r.querySelector('[data-testid="rich-note-property-row-value-input"]')!,
        wrapper.element
      )
      expectInside(
        rect(
          r.querySelector(
            '[data-testid="rich-note-property-value-dialog-open"]'
          )!
        ),
        rect(r)
      )
    })
  })

  it("follows the long labelled link from the expanded list in a narrow window", async () => {
    const wrapper = await mountExpandedListAtWidth(320)

    ;(
      wrapper.get(`${panelSelector} a.donut-wiki-link`).element as HTMLElement
    ).click()
    await flushPromises()

    expect(wrapper.vm.$router.currentRoute.value).toMatchObject(
      notePropertyLocation(42, "a part of")
    )
  })

  it("opens the shared tracker of a repeated expanded item in a narrow window", async () => {
    const wrapper = await mountExpandedListAtWidth(320)
    const trackerLinks = detailedRows(wrapper)
      .slice(2, 4)
      .map(
        (r) =>
          r.querySelector(
            '[data-test="assimilation-status-UNDERSTANDING"]'
          ) as HTMLElement
      )

    expect(trackerLinks[1]!.getAttribute("href")).toBe(
      trackerLinks[0]!.getAttribute("href")
    )
    trackerLinks[1]!.click()
    await flushPromises()

    expect(wrapper.vm.$router.currentRoute.value).toMatchObject({
      name: "memoryTrackerShow",
      params: { memoryTrackerId: "77" },
    })
  })

  it("assimilates the long unbroken expanded item in a narrow window", async () => {
    const assimilateSpy = mockSdkService(
      AssimilationController,
      "assimilate",
      []
    )
    const wrapper = await mountExpandedListAtWidth(320)

    await wrapper
      .get(`${panelSelector} button[aria-label="Assimilate ${unbroken}"]`)
      .trigger("click")
    await flushPromises()

    expect(assimilateSpy).toHaveBeenCalledWith({
      body: { noteId: 99, propertyKey: listKey, propertyValue: unbroken },
    })
  })
})
