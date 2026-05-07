import { describe, it, beforeEach, vi, expect } from "vitest"
import AssessmentHistoryPage from "@/pages/AssessmentHistoryPage.vue"
import helper, { mockSdkService } from "@tests/helpers"
import makeMe from "doughnut-test-fixtures/makeMe"
import { nextTick } from "vue"
import type { AssessmentAttempt } from "@generated/doughnut-backend-api"

vi.mock("vue-router", async (importOriginal) => {
  const actual = await importOriginal<typeof import("vue-router")>()
  return {
    ...actual,
    useRouter: () => ({
      push: vi.fn(),
    }),
  }
})

describe("assessment history page", () => {
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
      .component(AssessmentHistoryPage)
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
      'input[placeholder="Filter by notebook name"]'
    )
    await filterInput.setValue(assessmentForArt.notebookName)
    await nextTick()
    expect(wrapper.findAll("tr")).toHaveLength(2)
  })
})
