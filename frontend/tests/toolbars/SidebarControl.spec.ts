import SidebarControl from "@/components/toolbars/SidebarControl.vue"
import type { User } from "@/generated/backend/models/User"
import { fireEvent, screen } from "@testing-library/vue"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"
import { ref } from "vue"

const mockedPush = vi.fn()
vitest.mock("vue-router", () => ({
  useRoute: () => vi.fn().mockReturnValue(ref(null)),
  useRouter: () => ({
    push: mockedPush,
  }),
}))

describe("sidebar control", () => {
  let user: User

  beforeEach(() => {
    user = makeMe.aUser.please()
  })

  it("shows recent notes link in sidebar", async () => {
    helper.component(SidebarControl).withProps({ user }).render()
    await fireEvent.click(screen.getByRole("button", { name: "open sidebar" }))

    const recentLink = screen.getByRole("button", { name: "Recent Notes" })
    expect(recentLink).toBeInTheDocument()
  })

  it("shows circles link in sidebar", async () => {
    helper.component(SidebarControl).withProps({ user }).render()
    await fireEvent.click(screen.getByRole("button", { name: "open sidebar" }))

    const circlesLink = screen.getByRole("button", { name: "My Circles" })
    expect(circlesLink).toBeInTheDocument()
  })
})
