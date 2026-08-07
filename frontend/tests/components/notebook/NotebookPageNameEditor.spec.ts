import NotebookPageNameEditor from "@/components/notebook/NotebookPageNameEditor.vue"
import usePopups from "@/components/commons/Popups/usePopups"
import { flushPromises, mount } from "@vue/test-utils"
import { afterEach, describe, expect, it } from "vitest"

describe("NotebookPageNameEditor.vue", () => {
  afterEach(() => {
    while (usePopups().popups.peek()?.length) {
      usePopups().popups.done(false)
    }
  })

  it("shows a rename confirm that mentions wiki links from other notebooks", async () => {
    const wrapper = mount(NotebookPageNameEditor, {
      props: {
        notebookId: 1,
        name: "Original",
        settingsBody: { description: "", skipMemoryTrackingEntirely: false },
      },
    })

    await wrapper
      .get('[data-testid="notebook-page-name-edit"]')
      .trigger("click")
    const nameInput = wrapper.find('[data-test="notebook-page-name-input"]')
      .element as HTMLElement
    nameInput.innerText = "Renamed"
    nameInput.dispatchEvent(new Event("input", { bubbles: true }))
    await flushPromises()
    await wrapper
      .get('[data-testid="notebook-page-name-update"]')
      .trigger("click")
    await flushPromises()

    const stack = usePopups().popups.peek()
    expect(stack?.length).toBe(1)
    expect(stack?.[0]?.type).toBe("confirm")
    expect(stack?.[0]?.message).toContain(
      "wiki links from other notebooks to notes here may stop working"
    )
  })
})
