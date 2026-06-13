import {
  AssimilationController,
  NoteController,
} from "@generated/doughnut-backend-api/sdk.gen"
import AssimilationSettings from "@/components/recall/AssimilationSettings.vue"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import usePopups from "@/components/commons/Popups/usePopups"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

const mockedGoToNextAssimilation = vi.fn().mockResolvedValue(true)

vi.mock("@/composables/useGoToNextAssimilation", () => ({
  useGoToNextAssimilation: () => ({
    goToNextAssimilation: mockedGoToNextAssimilation,
  }),
}))

const noteRealm = makeMe.aNoteRealm
  .content(
    `---
topic: micronutrients
url: https://example.com
---

Vitamin notes body.`
  )
  .please()
const { note } = noteRealm

describe("AssimilationSettings", () => {
  let wrapper: VueWrapper
  let assimilateSpy: ReturnType<typeof mockSdkService>
  let getNoteInfoSpy: ReturnType<typeof mockSdkService>

  beforeEach(() => {
    mockedGoToNextAssimilation.mockClear()
    assimilateSpy = mockSdkService(AssimilationController, "assimilate", [])
    getNoteInfoSpy = mockSdkService(NoteController, "getNoteInfo", {})
  })

  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ""
    const popups = usePopups()
    while (popups.popups.peek().length) {
      popups.popups.done(false)
    }
  })

  const mountSettings = () => {
    wrapper = helper
      .component(AssimilationSettings)
      .withProps({
        note,
        noteInfoLoaded: true,
        keepForRecallDisabled: false,
      })
      .withRouter()
      .mount({ attachTo: document.body })
  }

  const expandPropertiesSection = async () => {
    const toggle = document.querySelector(
      '[data-test="assimilation-properties-toggle"]'
    ) as HTMLInputElement
    toggle.checked = true
    toggle.dispatchEvent(new Event("change", { bubbles: true }))
    await flushPromises()
  }

  it("renders a property row and Keep for recall control per frontmatter key", async () => {
    mountSettings()
    await flushPromises()
    await expandPropertiesSection()

    const rows = document.querySelectorAll(
      '[data-test="assimilation-property-row"]'
    )
    expect(rows).toHaveLength(2)
    expect(
      document.querySelector(
        '[data-test="assimilation-property-row"][data-property-key="topic"]'
      )
    ).not.toBeNull()
    expect(
      document.querySelector(
        '[data-test="assimilation-property-row"][data-property-key="url"]'
      )
    ).not.toBeNull()
    const keepForRecallControls = document.querySelectorAll(
      '[data-test="assimilation-property-row"] [data-test="keep-for-recall"]'
    )
    expect(keepForRecallControls).toHaveLength(2)
    for (const control of keepForRecallControls) {
      expect((control as HTMLInputElement).value).toBe("Keep for recall")
    }
  })

  it("calls assimilate with propertyKey and reloads note info", async () => {
    mountSettings()
    await flushPromises()
    await expandPropertiesSection()

    const topicRow = document.querySelector(
      '[data-test="assimilation-property-row"][data-property-key="topic"]'
    )!
    const keepForRecall = topicRow.querySelector(
      '[data-test="keep-for-recall"]'
    ) as HTMLInputElement
    keepForRecall.click()
    await flushPromises()

    expect(assimilateSpy).toHaveBeenCalledWith({
      body: { noteId: note.id, propertyKey: "topic" },
    })
    expect(getNoteInfoSpy.mock.calls.length).toBeGreaterThanOrEqual(2)
    expect(mockedGoToNextAssimilation).not.toHaveBeenCalled()
  })

  it("disables Keep for recall per property when a property memory tracker exists", async () => {
    getNoteInfoSpy.mockResolvedValue(
      wrapSdkResponse(
        makeMe.aNoteRecallInfo
          .memoryTrackers([
            {
              ...makeMe.aMemoryTracker.please(),
              id: 1,
              propertyKey: "topic",
            },
          ])
          .please()
      )
    )
    mountSettings()
    await flushPromises()
    await expandPropertiesSection()

    const topicKeepForRecall = document.querySelector(
      '[data-test="assimilation-property-row"][data-property-key="topic"] [data-test="keep-for-recall"]'
    ) as HTMLInputElement
    const urlKeepForRecall = document.querySelector(
      '[data-test="assimilation-property-row"][data-property-key="url"] [data-test="keep-for-recall"]'
    ) as HTMLInputElement

    expect(topicKeepForRecall.disabled).toBe(true)
    expect(urlKeepForRecall.disabled).toBe(false)
  })

  it("calls assimilate with skipMemoryTracking when skip recall is confirmed", async () => {
    assimilateSpy.mockResolvedValue(
      wrapSdkResponse([{ id: 1, removedFromTracking: true }])
    )
    mountSettings()
    await flushPromises()
    await expandPropertiesSection()

    const topicRow = document.querySelector(
      '[data-test="assimilation-property-row"][data-property-key="topic"]'
    )!
    const skipRecall = topicRow.querySelector(
      '[value="Skip recall"]'
    ) as HTMLInputElement
    skipRecall.click()
    await flushPromises()

    const popupStack = usePopups().popups.peek()
    expect(popupStack).toHaveLength(1)
    expect(popupStack?.[0]?.type).toBe("confirm")
    expect(popupStack?.[0]?.message).toBe(
      "Confirm to hide this property from recalls in the future?"
    )

    usePopups().popups.done(true)
    await flushPromises()

    expect(assimilateSpy).toHaveBeenCalledWith({
      body: {
        noteId: note.id,
        propertyKey: "topic",
        skipMemoryTracking: true,
      },
    })
    expect(getNoteInfoSpy.mock.calls.length).toBeGreaterThanOrEqual(2)
    expect(mockedGoToNextAssimilation).toHaveBeenCalled()
  })
})
