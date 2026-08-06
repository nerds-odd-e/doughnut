import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import usePopups from "@/components/commons/Popups/usePopups"
import { wrapSdkResponse } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { describe, it, expect, vi } from "vitest"
import {
  aNotebook,
  mountNotebookPageView,
  stubNotebookPageViewBookAbsent,
} from "./notebookPageViewTestSupport"

describe("NotebookPageView settings", () => {
  stubNotebookPageViewBookAbsent()

  it("sends description when saving notebook description", async () => {
    const nb = aNotebook({ description: "Initial blurb" })
    const updateSpy = vi
      .spyOn(NotebookController, "updateNotebook")
      .mockResolvedValue(wrapSdkResponse({ ...nb, description: "Saved blurb" }))
    const wrapper = mountNotebookPageView(nb)

    await wrapper
      .get('[data-testid="notebook-workspace-tab-settings"]')
      .trigger("click")
    await flushPromises()

    await wrapper.find("[name='description']").setValue("Saved blurb")
    await wrapper
      .get('[data-testid="notebook-description-save"]')
      .trigger("click")
    await flushPromises()

    expect(updateSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        path: { notebook: nb.id },
        body: expect.objectContaining({
          description: "Saved blurb",
          skipMemoryTrackingEntirely:
            nb.notebookSettings.skipMemoryTrackingEntirely,
        }),
      })
    )
    updateSpy.mockRestore()
  })

  it("auto-saves when skip memory tracking is toggled", async () => {
    const nb = aNotebook({
      notebookSettings: { skipMemoryTrackingEntirely: false },
    })
    const updateSpy = vi
      .spyOn(NotebookController, "updateNotebook")
      .mockResolvedValue(
        wrapSdkResponse({
          ...nb,
          notebookSettings: { skipMemoryTrackingEntirely: true },
        })
      )
    const wrapper = mountNotebookPageView(nb)

    await wrapper
      .get('[data-testid="notebook-workspace-tab-settings"]')
      .trigger("click")
    await flushPromises()

    await wrapper.find("[name='skipMemoryTrackingEntirely']").setValue(true)
    await flushPromises()

    expect(updateSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        path: { notebook: nb.id },
        body: expect.objectContaining({
          skipMemoryTrackingEntirely: true,
          description: nb.description ?? "",
        }),
      })
    )
    updateSpy.mockRestore()
  })

  it("sends current settings and new name when updating notebook name from summary", async () => {
    const nb = aNotebook({ name: "Original title" })
    const updateSpy = vi
      .spyOn(NotebookController, "updateNotebook")
      .mockResolvedValue(wrapSdkResponse({ ...nb, name: "Edited title" }))
    const wrapper = mountNotebookPageView(nb)

    await wrapper
      .get('[data-testid="notebook-page-name-edit"]')
      .trigger("click")
    const nameInput = wrapper.find('[data-test="notebook-page-name-input"]')
      .element as HTMLElement
    nameInput.innerText = "Edited title"
    nameInput.dispatchEvent(new Event("input", { bubbles: true }))
    await flushPromises()
    await wrapper
      .get('[data-testid="notebook-page-name-update"]')
      .trigger("click")
    await flushPromises()
    while (usePopups().popups.peek()?.length) {
      usePopups().popups.done(true)
      await flushPromises()
    }

    expect(updateSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        path: { notebook: nb.id },
        body: expect.objectContaining({
          name: "Edited title",
          description: nb.description ?? "",
          skipMemoryTrackingEntirely:
            nb.notebookSettings.skipMemoryTrackingEntirely,
        }),
      })
    )
    updateSpy.mockRestore()
  })
})
