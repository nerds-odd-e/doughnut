import SidebarControl from "@/components/toolbars/SidebarControl.vue"
import type { User } from "@/generated/backend/models/User"
import { screen } from "@testing-library/vue"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"

const useRouteValue = { name: "" }
vitest.mock("vue-router", () => ({
  useRoute: () => useRouteValue,
}))

describe("sidebar control", () => {
  let user: User

  beforeEach(() => {
    user = makeMe.aUser.please()
  })

  it("shows recent notes link in sidebar", async () => {
    helper.component(SidebarControl).withProps({ user }).render()

    const recentLink = screen.getByRole("button", { name: "Recent" })
    expect(recentLink).toBeInTheDocument()
  })

  it("shows circles link in sidebar", async () => {
    helper.component(SidebarControl).withProps({ user }).render()

    const circlesLink = screen.getByRole("button", { name: "Circles" })
    expect(circlesLink).toBeInTheDocument()
  })

  it("highlights the notebooks link when on notebooks page", () => {
    useRouteValue.name = "notebooks"

    helper.component(SidebarControl).withProps({ user }).render()

    const notebooksLink = screen.getByRole("button", { name: "Notebooks" })
    expect(notebooksLink).toHaveClass("active")
  })

  it("highlights the circles link when on circle show page", () => {
    useRouteValue.name = "circleShow"

    helper.component(SidebarControl).withProps({ user }).render()

    const circlesLink = screen.getByRole("button", { name: "Circles" })
    expect(circlesLink).toHaveClass("active")
  })
})
