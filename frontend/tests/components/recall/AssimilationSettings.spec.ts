import makeMe from "doughnut-test-fixtures/makeMe"
import { wrapSdkResponse } from "@tests/helpers"
import { assimilateButtonSelector } from "./assimilationPanelTestSupport"
import {
  assimilationPropertyRow,
  clickPropertyAssimilate,
  clickPropertyRevive,
  clickPropertySkipRecall,
  getNoteInfoSpy,
  mountAssimilationSettingsReady,
  propertyAssimilateButton,
  propertyReviveButton,
  propertySkipRecallButton,
  setupAssimilationSettingsTests,
  wrapper,
} from "./assimilationSettingsTestSupport"
import { beforeEach, describe, expect, it } from "vitest"

setupAssimilationSettingsTests()

describe("AssimilationSettings", () => {
  it("renders property rows with Assimilate controls and emits assimilate with propertyKey", async () => {
    await mountAssimilationSettingsReady()

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

    await clickPropertyAssimilate("topic")
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
    await mountAssimilationSettingsReady()

    expect(propertyAssimilateButton("topic").disabled).toBe(true)
    expect(propertyAssimilateButton("url").disabled).toBe(false)
  })

  it("emits assimilate with skipMemoryTracking and propertyKey when skip recall is clicked", async () => {
    await mountAssimilationSettingsReady()

    await clickPropertySkipRecall("topic")
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

    it("shows Revive on skipped property and emits revive when clicked", async () => {
      await mountAssimilationSettingsReady()

      expect(propertyReviveButton("topic")).not.toBeNull()
      expect(propertySkipRecallButton("topic")).toBeNull()
      expect(propertySkipRecallButton("url")).not.toBeNull()
      expect(propertyReviveButton("url")).toBeNull()

      await clickPropertyRevive("topic")
      expect(wrapper.emitted("revive")).toEqual([[{ propertyKey: "topic" }]])
    })
  })
})
