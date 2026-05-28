import AccountMenuItem from "@/components/toolbars/AccountMenuItem.vue"
import helper from "@tests/helpers"
import { fireEvent, screen } from "@testing-library/vue"
import makeMe from "doughnut-test-fixtures/makeMe"
import { flushPromises } from "@vue/test-utils"
import { describe, expect, it, vi } from "vitest"

describe("AccountMenuItem", () => {
  const renderAccountMenuItem = (overrides?: {
    logout?: () => void
    showUserSettingsDialog?: () => void
  }) =>
    helper
      .component(AccountMenuItem)
      .withProps({
        user: makeMe.aUser.please(),
        showUserSettingsDialog: overrides?.showUserSettingsDialog ?? vi.fn(),
        logout: overrides?.logout ?? vi.fn(),
      })
      .withRouter()
      .render()

  it("calls logout when the user chooses Logout", async () => {
    const logout = vi.fn()
    renderAccountMenuItem({ logout })

    await fireEvent.click(screen.getByLabelText("Account"))
    await flushPromises()
    const logoutLink = Array.from(
      document.querySelectorAll("[data-dropdown-portal-panel] a")
    ).find((el) => el.textContent?.includes("Logout"))
    expect(logoutLink).toBeDefined()
    await fireEvent.click(logoutLink as HTMLElement)
    expect(logout).toHaveBeenCalledOnce()
  })

  it("opens account actions when the browser click targets the details root", async () => {
    renderAccountMenuItem()

    const dropdown = document.querySelector("[data-auto-collapse-dropdown]")
    expect(dropdown).toBeInstanceOf(HTMLDetailsElement)

    await fireEvent.click(dropdown as HTMLDetailsElement)

    expect((dropdown as HTMLDetailsElement).open).toBe(true)
  })
})
