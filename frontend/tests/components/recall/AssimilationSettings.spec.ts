import { NoteController } from "@generated/doughnut-backend-api/sdk.gen"
import AssimilationSettings from "@/components/recall/AssimilationSettings.vue"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import {
  assimilationPropertyRow,
  expandAssimilationPropertiesSection,
  noteWithAssimilationProperties,
} from "./assimilationPropertyTestSupport"
import { assimilateButtonSelector } from "./assimilationPanelTestSupport"
import { afterEach, beforeEach, describe, expect, it } from "vitest"

describe("AssimilationSettings", () => {
  let wrapper: VueWrapper
  let getNoteInfoSpy: ReturnType<typeof mockSdkService>

  beforeEach(() => {
    getNoteInfoSpy = mockSdkService(NoteController, "getNoteInfo", {})
  })

  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ""
  })

  const mountSettings = () => {
    wrapper = helper
      .component(AssimilationSettings)
      .withProps({
        note: noteWithAssimilationProperties,
        noteInfoLoaded: true,
        assimilateDisabled: false,
      })
      .withRouter()
      .mount({ attachTo: document.body })
  }

  it("renders a property row and Assimilate control per frontmatter key", async () => {
    mountSettings()
    await flushPromises()
    await expandAssimilationPropertiesSection()

    const rows = document.querySelectorAll(
      '[data-test="assimilation-property-row"]'
    )
    expect(rows).toHaveLength(2)
    expect(assimilationPropertyRow("topic")).not.toBeNull()
    expect(assimilationPropertyRow("url")).not.toBeNull()
    const assimilateControls = document.querySelectorAll(
      `[data-test="assimilation-property-row"] ${assimilateButtonSelector}`
    )
    expect(assimilateControls).toHaveLength(2)
    for (const control of assimilateControls) {
      expect((control as HTMLInputElement).value).toBe("Assimilate")
    }
  })

  it("emits assimilate with propertyKey when assimilating a property", async () => {
    mountSettings()
    await flushPromises()
    await expandAssimilationPropertiesSection()

    const assimilate = assimilationPropertyRow("topic").querySelector(
      assimilateButtonSelector
    ) as HTMLInputElement
    assimilate.click()
    await flushPromises()

    expect(wrapper.emitted("assimilate")).toEqual([
      [{ skipMemoryTracking: false, propertyKey: "topic" }],
    ])
  })

  it("disables Assimilate per property when a property memory tracker exists", async () => {
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
    await expandAssimilationPropertiesSection()

    const topicAssimilate = assimilationPropertyRow("topic").querySelector(
      assimilateButtonSelector
    ) as HTMLInputElement
    const urlAssimilate = assimilationPropertyRow("url").querySelector(
      assimilateButtonSelector
    ) as HTMLInputElement

    expect(topicAssimilate.disabled).toBe(true)
    expect(urlAssimilate.disabled).toBe(false)
  })

  it("emits assimilate with skipMemoryTracking and propertyKey when skip recall is clicked", async () => {
    mountSettings()
    await flushPromises()
    await expandAssimilationPropertiesSection()

    const skipRecall = assimilationPropertyRow("topic").querySelector(
      '[value="Skip recall"]'
    ) as HTMLInputElement
    skipRecall.click()
    await flushPromises()

    expect(wrapper.emitted("assimilate")).toEqual([
      [{ skipMemoryTracking: true, propertyKey: "topic" }],
    ])
  })

  describe("skipped property tracker", () => {
    beforeEach(() => {
      getNoteInfoSpy.mockResolvedValue(
        wrapSdkResponse(
          makeMe.aNoteRecallInfo
            .memoryTrackers([
              {
                ...makeMe.aMemoryTracker.please(),
                id: 1,
                propertyKey: "topic",
                removedFromTracking: true,
              },
            ])
            .please()
        )
      )
    })

    it("shows Revive instead of Skip recall when property tracker is skipped", async () => {
      mountSettings()
      await flushPromises()
      await expandAssimilationPropertiesSection()

      const topicRow = assimilationPropertyRow("topic")
      expect(topicRow.querySelector('[value="Revive"]')).not.toBeNull()
      expect(topicRow.querySelector('[value="Skip recall"]')).toBeNull()

      const urlRow = assimilationPropertyRow("url")
      expect(urlRow.querySelector('[value="Skip recall"]')).not.toBeNull()
      expect(urlRow.querySelector('[value="Revive"]')).toBeNull()
    })

    it("emits revive with propertyKey when Revive is clicked on a skipped property", async () => {
      mountSettings()
      await flushPromises()
      await expandAssimilationPropertiesSection()

      const revive = assimilationPropertyRow("topic").querySelector(
        '[data-test="revive"]'
      ) as HTMLInputElement
      revive.click()
      await flushPromises()

      expect(wrapper.emitted("revive")).toEqual([[{ propertyKey: "topic" }]])
    })
  })
})
