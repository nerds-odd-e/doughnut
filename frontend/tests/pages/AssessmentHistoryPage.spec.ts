import { describe, it } from "vitest"
import AssessmentHistoryPage from "@/pages/AssessmentHistoryPage.vue"
import helper from "../helpers"
import makeMe from "../fixtures/makeMe"
import { nextTick } from "vue"

describe("assessment history page", () => {
  const user = makeMe.aUser.please()
  let wrapper

  beforeEach(() => {
    helper.managedApi.restAssessmentController.getAssessmentHistory = vi
      .fn()
      .mockResolvedValue([])
    wrapper = helper
      .component(AssessmentHistoryPage)
      .withProps({ user })
      .mount()
  })

  it("calls API ONCE on mount", async () => {
    expect(
      helper.managedApi.restAssessmentController.getAssessmentHistory
    ).toBeCalledTimes(1)
  })

  it("filters by text", async () => {
    const filterInput = wrapper.find(
      'input[placeholder="Filter by notebook title"]'
    )
    await filterInput.setValue("test text")
    await nextTick()
  })

  it("filters by certificate", async () => {
    const certificateDropdown = wrapper.find(
      'input[type="checkbox"]#filterByCertificate'
    )
    await certificateDropdown.setValue("certificate-id")
    await nextTick()
  })
})
