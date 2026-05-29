import { defineComponent, markRaw } from "vue"
import { describe, it, expect } from "vitest"
import helper from "@tests/helpers"
import DropdownMenuActionButton from "@/components/commons/DropdownMenuActionButton.vue"

const stubIcon = markRaw(
  defineComponent({
    template: '<span data-testid="action-icon" />',
  })
)

describe("DropdownMenuActionButton", () => {
  it("renders icon and title without a check by default", () => {
    const wrapper = helper
      .component(DropdownMenuActionButton)
      .withProps({ title: "Test action", icon: stubIcon })
      .mount()

    expect(
      wrapper.find('[data-testid="dropdown-menu-action-checked"]').exists()
    ).toBe(false)
    expect(wrapper.find('[data-testid="action-icon"]').exists()).toBe(true)
    expect(wrapper.text()).toContain("Test action")
  })

  it("renders no check when checked is explicitly false", () => {
    const wrapper = helper
      .component(DropdownMenuActionButton)
      .withProps({ title: "Test action", icon: stubIcon, checked: false })
      .mount()

    expect(
      wrapper.find('[data-testid="dropdown-menu-action-checked"]').exists()
    ).toBe(false)
  })

  it("renders check before the action icon when checked", () => {
    const wrapper = helper
      .component(DropdownMenuActionButton)
      .withProps({ title: "Test action", icon: stubIcon, checked: true })
      .mount()

    const check = wrapper.find('[data-testid="dropdown-menu-action-checked"]')
    const actionIcon = wrapper.find('[data-testid="action-icon"]')
    expect(check.exists()).toBe(true)
    expect(actionIcon.exists()).toBe(true)

    const checkEl = check.element
    const iconEl = actionIcon.element
    expect(
      checkEl.compareDocumentPosition(iconEl) & Node.DOCUMENT_POSITION_FOLLOWING
    ).toBeTruthy()
  })

  it("emits click when the button is clicked", async () => {
    const wrapper = helper
      .component(DropdownMenuActionButton)
      .withProps({ title: "Test action", icon: stubIcon })
      .mount()

    await wrapper.find("button").trigger("click")
    expect(wrapper.emitted("click")).toHaveLength(1)
  })
})
