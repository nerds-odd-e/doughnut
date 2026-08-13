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
  it("renders icon and title", () => {
    const wrapper = helper
      .component(DropdownMenuActionButton)
      .withProps({ title: "Test action", icon: stubIcon })
      .mount()

    expect(wrapper.find('[data-testid="action-icon"]').exists()).toBe(true)
    expect(wrapper.text()).toContain("Test action")
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
