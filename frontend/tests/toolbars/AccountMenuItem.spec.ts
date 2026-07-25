import AccountMenuItem from "@/components/toolbars/AccountMenuItem.vue"
import helper from "@tests/helpers"
import { fireEvent, screen } from "@testing-library/vue"
import makeMe from "doughnut-test-fixtures/makeMe"
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
})
