import AccountMenuItem from "@/components/toolbars/AccountMenuItem.vue"
import helper from "@tests/helpers"
import { fireEvent, screen } from "@testing-library/vue"
import makeMe from "donut-test-fixtures/makeMe"
import { flushPromises } from "@vue/test-utils"
import { describe, expect, it, vi } from "vitest"

describe("AccountMenuItem", () => {
  const renderAccountMenuItem = (overrides?: { logout?: () => void }) =>
    helper
      .component(AccountMenuItem)
      .withProps({
        user: makeMe.aUser.please(),
        logout: overrides?.logout ?? vi.fn(),
      })
      .withRouter()
      .render()

  it("calls logout when the user chooses Logout", async () => {
    const logout = vi.fn()
    renderAccountMenuItem({ logout })

    await fireEvent.click(screen.getByLabelText("Account"))
    await flushPromises()
    await fireEvent.click(screen.getByTitle("Logout"))
    expect(logout).toHaveBeenCalledOnce()
  })

  it("shows its actions while expanded and hides them when pressed again", async () => {
    renderAccountMenuItem()
    const account = screen.getByRole("button", { name: "Account" })
    expect(account).toHaveAttribute("aria-expanded", "false")
    expect(screen.queryByTitle("Logout")).toBeNull()

    await fireEvent.click(account)
    expect(account).toHaveAttribute("aria-expanded", "true")
    expect(screen.getByTitle("Logout")).toBeInTheDocument()

    await fireEvent.click(account)
    expect(account).toHaveAttribute("aria-expanded", "false")
    expect(screen.queryByTitle("Logout")).toBeNull()
  })
})
