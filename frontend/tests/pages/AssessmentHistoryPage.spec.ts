import { describe, it, beforeEach, vi } from "vitest"
import AssessmentAndCertificateHistoryPage from "@/pages/AssessmentAndCertificateHistoryPage.vue"
import helper, { mockSdkService } from "@tests/helpers"
import makeMe from "@tests/fixtures/makeMe"
import { nextTick } from "vue"
import type { AssessmentAttempt } from "@generated/backend"

vitest.mock("vue-router", () => ({
  useRouter: () => ({
    push: vi.fn(),
  }),
}))

describe("assessment and certificate history page", () => {
  const user = makeMe.aUser.please()
  const assessmentForArt: AssessmentAttempt =
    makeMe.anAssessmentAttempt.please()
  const assessmentForTech: AssessmentAttempt = makeMe.anAssessmentAttempt
    .passed()
    .please()
  let wrapper
  let getMyAssessmentsSpy: ReturnType<typeof mockSdkService<"getMyAssessments">>

  beforeEach(() => {
    getMyAssessmentsSpy = mockSdkService("getMyAssessments", [
      assessmentForArt,
      assessmentForTech,
    ])
    wrapper = helper
      .component(AssessmentAndCertificateHistoryPage)
      .withCurrentUser(user)
      .mount()
  })

  it("calls API ONCE on mount", async () => {
    expect(getMyAssessmentsSpy).toBeCalledTimes(1)
  })

  it("should have two items in the list", async () => {
    expect(wrapper.findAll("tr")).toHaveLength(3)
  })

  it("filters by text", async () => {
    const filterInput = wrapper.find(
      'input[placeholder="Filter by notebook title"]'
    )
    await filterInput.setValue(assessmentForArt.notebookTitle)
    await nextTick()
    expect(wrapper.findAll("tr")).toHaveLength(2)
  })

  it("filters by certificate", async () => {
    const certificateDropdown = wrapper.find(
      'input[type="checkbox"]#filterByCertificate'
    )
    await certificateDropdown.setValue("certificate-id")
    await nextTick()
    expect(wrapper.findAll("tr")).toHaveLength(2)
  })
})
