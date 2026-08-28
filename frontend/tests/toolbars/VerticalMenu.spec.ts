import VerticalMenu from "@/components/toolbars/VerticalMenu.vue"
import helper from "@tests/helpers"
import { describe, expect, it } from "vitest"

describe("VerticalMenu", () => {
  it("shows Donut brand", () => {
    const wrapper = helper
      .component(VerticalMenu)
      .withProps({
        upperNavItems: [],
        lowerNavItems: [],
        isHomePage: true,
        logout: () => undefined,
      })
      .withRouter()
      .mount()

    expect(wrapper.text()).toContain("Donut by")
  })
})
