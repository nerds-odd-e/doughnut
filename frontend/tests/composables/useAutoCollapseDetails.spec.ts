import { useAutoCollapseDetails } from "@/composables/useAutoCollapseDetails"
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

  it("does not close when click target is inside another open dialog", async () => {
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
    expect(details.open).toBe(true)

    const dialog = document.createElement("dialog")
    dialog.className = "modal-mask"
    const copyButton = document.createElement("button")
    copyButton.type = "button"
    const icon = document.createElementNS("http://www.w3.org/2000/svg", "svg")
    copyButton.append(icon)
    dialog.append(copyButton)
    document.body.append(dialog)
    dialog.showModal()

    icon.dispatchEvent(
      new MouseEvent("click", { bubbles: true, cancelable: true })
    )

    expect(details.open).toBe(true)
    dialog.close()
    wrapper.unmount()
  })

  it("treats a click in a portaled panel owned by a nested dropdown as inside", async () => {
    const Harness = defineComponent({
      setup() {
        const detailsRef = ref<HTMLDetailsElement | null>(null)
        useAutoCollapseDetails(detailsRef, undefined, "outer-portal-id")
        return { detailsRef }
      },
      template: `
        <details ref="detailsRef" open>
          <summary>Trigger</summary>
          <details data-dropdown-portal-owner="nested-portal-id" />
        </details>
      `,
    })

    const wrapper = mount(Harness, { attachTo: document.body })
    const details = wrapper.find("details").element as HTMLDetailsElement
    const clickInPanelFor = (portalId: string) => {
      const panel = document.createElement("ul")
      panel.setAttribute("data-dropdown-portal-for", portalId)
      const item = document.createElement("button")
      item.type = "button"
      panel.append(item)
      document.body.append(panel)
      item.dispatchEvent(
        new MouseEvent("click", { bubbles: true, cancelable: true })
      )
    }

    clickInPanelFor("nested-portal-id")
    expect(details.open).toBe(true)

    clickInPanelFor("unrelated-portal-id")
    expect(details.open).toBe(false)
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
