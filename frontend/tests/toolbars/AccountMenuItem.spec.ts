import AccountMenuItem from "@/components/toolbars/AccountMenuItem.vue"
import helper from "@tests/helpers"
import { fireEvent, screen } from "@testing-library/vue"
import makeMe from "doughnut-test-fixtures/makeMe"
import { describe, expect, it, vi } from "vitest"

describe("AccountMenuItem", () => {
  it("calls logout when the user chooses Logout", async () => {
    const logout = vi.fn()
    const showUserSettingsDialog = vi.fn()
    helper
      .component(AccountMenuItem)
      .withProps({
        user: makeMe.aUser.please(),
        showUserSettingsDialog,
        logout,
      })
      .withRouter()
      .render()

    await fireEvent.click(screen.getByLabelText("Account"))
    await fireEvent.click(screen.getByText("Logout"))
    expect(logout).toHaveBeenCalledOnce()
  })
})
