import NewBookBlockTitleDialog from "@/components/book-reading/NewBookBlockTitleDialog.vue"
import helper from "@tests/helpers"
import { userEvent } from "vitest/browser"
import { describe, expect, it } from "vitest"

describe("NewBookBlockTitleDialog", () => {
  const openDialog = async () => {
    const wrapper = helper
      .component(NewBookBlockTitleDialog)
      .withProps({ open: false, defaultTitle: "Chapter 1" })
      .mount({ attachTo: document.body })
    await wrapper.setProps({ open: true })
    return wrapper
  }

  it("focuses the input with the whole default title selected", async () => {
    const wrapper = await openDialog()
    const input = wrapper.find<HTMLInputElement>(
      '[data-testid="new-block-title-input"]'
    ).element

    expect(document.activeElement).toBe(input)
    expect(input.selectionStart).toBe(0)
    expect(input.selectionEnd).toBe("Chapter 1".length)
    wrapper.unmount()
  })

  it("confirms the typed title on Enter", async () => {
    const wrapper = await openDialog()

    await userEvent.keyboard("Intro{Enter}")

    expect(wrapper.emitted("confirm")).toEqual([["Intro"]])
    wrapper.unmount()
  })

  it("cancels without confirming on Escape", async () => {
    const wrapper = await openDialog()

    await userEvent.keyboard("{Escape}")

    expect(wrapper.emitted("cancel")).toHaveLength(1)
    expect(wrapper.emitted("confirm")).toBeUndefined()
    wrapper.unmount()
  })
})
