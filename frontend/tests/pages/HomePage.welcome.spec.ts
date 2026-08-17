import HomePage from "@/pages/HomePage.vue"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper from "@tests/helpers"
import { describe, expect, it } from "vitest"

describe("HomePage welcome copy", () => {
  it("describes capture, assimilate, and recall", () => {
    const wrapper = helper
      .component(HomePage)
      .withRouter()
      .withCurrentUser(makeMe.aUser.please())
      .mount()

    const text = wrapper.text()
    expect(text).toContain("Your notebooks grow as you capture")
    expect(text).toContain("as you recall")
  })
})
