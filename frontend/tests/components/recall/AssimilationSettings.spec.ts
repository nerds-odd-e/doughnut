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
        keepForRecallDisabled: false,
      })
      .withRouter()
      .mount({ attachTo: document.body })
  }

  it("renders a property row and Keep for recall control per frontmatter key", async () => {
    mountSettings()
    await flushPromises()
    await expandAssimilationPropertiesSection()

    const rows = document.querySelectorAll(
      '[data-test="assimilation-property-row"]'
    )
    expect(rows).toHaveLength(2)
    expect(assimilationPropertyRow("topic")).not.toBeNull()
    expect(assimilationPropertyRow("url")).not.toBeNull()
    const keepForRecallControls = document.querySelectorAll(
      '[data-test="assimilation-property-row"] [data-test="keep-for-recall"]'
    )
    expect(keepForRecallControls).toHaveLength(2)
    for (const control of keepForRecallControls) {
      expect((control as HTMLInputElement).value).toBe("Keep for recall")
    }
  })

  it("emits assimilate with propertyKey when keeping a property for recall", async () => {
    mountSettings()
    await flushPromises()
    await expandAssimilationPropertiesSection()

    const keepForRecall = assimilationPropertyRow("topic").querySelector(
      '[data-test="keep-for-recall"]'
    ) as HTMLInputElement
    keepForRecall.click()
    await flushPromises()

    expect(wrapper.emitted("assimilate")).toEqual([
      [{ skipMemoryTracking: false, propertyKey: "topic" }],
    ])
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
    await expandAssimilationPropertiesSection()

    const topicKeepForRecall = assimilationPropertyRow("topic").querySelector(
      '[data-test="keep-for-recall"]'
    ) as HTMLInputElement
    const urlKeepForRecall = assimilationPropertyRow("url").querySelector(
      '[data-test="keep-for-recall"]'
    ) as HTMLInputElement

    expect(topicKeepForRecall.disabled).toBe(true)
    expect(urlKeepForRecall.disabled).toBe(false)
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
})
