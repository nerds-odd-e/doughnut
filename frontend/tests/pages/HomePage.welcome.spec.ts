import { AssimilationController } from "@generated/donut-backend-api/sdk.gen"
import HomePage from "@/pages/HomePage.vue"
import { noteShowLocation } from "@/routes/noteShowLocation"
import { screen } from "@testing-library/vue"
import makeMe from "donut-test-fixtures/makeMe"
import helper, { mockSdkService, productionRouterAt } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { describe, expect, it } from "vitest"
import { userEvent } from "vitest/browser"

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

  it("uses Donut in the fallback welcome and tagline", () => {
    const wrapper = helper.component(HomePage).withRouter().mount()

    const text = wrapper.text()
    expect(text).toContain("To Donut")
    expect(text).toContain("Donut will eventually")
  })

  it("does not draw the flow line while the background is hidden", async () => {
    const wrapper = helper
      .component(HomePage)
      .withRouter()
      .withCurrentUser(makeMe.aUser.please())
      .mount({ attachTo: document.body })

    await new Promise((resolve) => setTimeout(resolve, 150))

    expect(wrapper.find(".flow-path").attributes("d")).toBeUndefined()
    wrapper.unmount()
  })

  it("goes to the next note to assimilate from the welcome's Assimilate item", async () => {
    mockSdkService(AssimilationController, "next", { nextUnit: { noteId: 7 } })
    const router = await productionRouterAt({ name: "root" })
    helper
      .component(HomePage)
      .withRealRouter(router)
      .withCurrentUser(makeMe.aUser.please())
      .render()

    await userEvent.click(screen.getByLabelText("Assimilate"))
    await flushPromises()

    expect(router.currentRoute.value).toMatchObject(noteShowLocation(7))
  })
})
