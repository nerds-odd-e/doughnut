import { useAutoCollapseDetails } from "@/composables/useAutoCollapseDetails"
import { DROPDOWN_PORTAL_PANEL_ATTR } from "@/composables/dropdownPortalContext"
import { mount } from "@vue/test-utils"
import { defineComponent, ref } from "vue"
import { afterEach, describe, expect, it } from "vitest"

describe("useAutoCollapseDetails", () => {
  afterEach(() => {
    document.body.innerHTML = ""
  })

  it("does not close when click target is inside the portaled panel", async () => {
    const Harness = defineComponent({
      setup() {
        const detailsRef = ref<HTMLDetailsElement | null>(null)
        const portalId = "test-portal-id"
        const closeDetails = () => {
          if (detailsRef.value) detailsRef.value.open = false
        }

        useAutoCollapseDetails(detailsRef, closeDetails, portalId)

        return { detailsRef, portalId }
      },
      template: `
        <details ref="detailsRef" open>
          <summary>Trigger</summary>
        </details>
      `,
    })

    const wrapper = mount(Harness, { attachTo: document.body })
    const details = wrapper.find("details").element as HTMLDetailsElement
    expect(details.open).toBe(true)

    const portaledPanel = document.createElement("ul")
    portaledPanel.setAttribute(DROPDOWN_PORTAL_PANEL_ATTR, "")
    portaledPanel.setAttribute("data-dropdown-portal-for", "test-portal-id")
    const menuButton = document.createElement("button")
    menuButton.type = "button"
    menuButton.textContent = "Menu action"
    portaledPanel.append(menuButton)
    document.body.append(portaledPanel)

    menuButton.dispatchEvent(
      new MouseEvent("click", { bubbles: true, cancelable: true })
    )

    expect(details.open).toBe(true)
    wrapper.unmount()
  })

  it("closes when click target is outside details and portaled panel", async () => {
    const Harness = defineComponent({
      setup() {
        const detailsRef = ref<HTMLDetailsElement | null>(null)
        const portalId = "test-portal-id"
        const closeDetails = () => {
          if (detailsRef.value) detailsRef.value.open = false
        }

        useAutoCollapseDetails(detailsRef, closeDetails, portalId)

        return { detailsRef }
      },
      template: `
        <details ref="detailsRef" open>
          <summary>Trigger</summary>
        </details>
      `,
    })

    const wrapper = mount(Harness, { attachTo: document.body })
    const details = wrapper.find("details").element as HTMLDetailsElement

    const outside = document.createElement("button")
    outside.type = "button"
    document.body.append(outside)

    outside.dispatchEvent(
      new MouseEvent("click", { bubbles: true, cancelable: true })
    )

    expect(details.open).toBe(false)
    wrapper.unmount()
  })
})
