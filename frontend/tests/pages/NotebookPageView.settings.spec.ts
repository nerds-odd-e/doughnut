import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import usePopups from "@/components/commons/Popups/usePopups"
import { wrapSdkError, wrapSdkResponse } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { afterEach, describe, it, expect, vi } from "vitest"
import {
  aNotebook,
  mountNotebookPageView,
  stubNotebookPageViewBookAbsent,
} from "./notebookPageViewTestSupport"
import { editPageName } from "./pageNameEditorTestSupport"

describe("NotebookPageView settings", () => {
  stubNotebookPageViewBookAbsent()

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it("sends description when saving notebook description", async () => {
    const nb = aNotebook({ description: "Initial blurb" })
    const updateSpy = vi
      .spyOn(NotebookController, "updateNotebook")
      .mockResolvedValue(wrapSdkResponse({ ...nb, description: "Saved blurb" }))
    const wrapper = mountNotebookPageView(nb)

    await wrapper.get('[data-testid="notebook-tab-settings"]').trigger("click")
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

    await wrapper.get('[data-testid="notebook-tab-settings"]').trigger("click")
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
  })

  it("saves a trimmed heading with current settings and emits the updated notebook", async () => {
    const nb = aNotebook({
      name: "Original title",
      description: "Current description",
      notebookSettings: { skipMemoryTrackingEntirely: true },
    })
    const updatedNotebook = { ...nb, name: "Edited title" }
    const updateSpy = vi
      .spyOn(NotebookController, "updateNotebook")
      .mockResolvedValue(wrapSdkResponse(updatedNotebook))
    const wrapper = mountNotebookPageView(nb)

    await editPageName(wrapper, "notebook-page-name", "  Edited title  ")

    expect(updateSpy).toHaveBeenCalledWith({
      path: { notebook: nb.id },
      body: {
        name: "Edited title",
        description: "Current description",
        skipMemoryTrackingEntirely: true,
      },
    })
    expect(wrapper.emitted("notebook-updated")).toEqual([[updatedNotebook]])
  })

  it("keeps the wiki-link risk visible without offering the old rename workflow", () => {
    const wrapper = mountNotebookPageView(aNotebook({ name: "Original" }))
    const summary = wrapper.get('[data-testid="notebook-page-summary"]')

    expect(wrapper.text()).toContain(
      "wiki links from other notebooks to notes here may stop working"
    )
    expect(
      wrapper
        .get('[data-test="notebook-page-name"]')
        .attributes("contenteditable")
    ).toBe("true")
    expect(summary.findAll("button")).toHaveLength(0)
    expect(summary.text()).not.toContain("Update")
    expect(summary.text()).not.toContain("Cancel")
    expect(usePopups().popups.peek()).toHaveLength(0)
  })

  it("does not save blank or unchanged headings", async () => {
    const nb = aNotebook({ name: "Original" })
    const updateSpy = vi.spyOn(NotebookController, "updateNotebook")
    const wrapper = mountNotebookPageView(nb)

    await editPageName(wrapper, "notebook-page-name", "   ")

    expect(wrapper.text()).toContain("Notebook name cannot be empty")

    await editPageName(wrapper, "notebook-page-name", "Original")

    expect(updateSpy).not.toHaveBeenCalled()
  })

  it("keeps API rename errors inline with the unsaved heading", async () => {
    const nb = aNotebook({ name: "Original" })
    vi.spyOn(NotebookController, "updateNotebook").mockResolvedValue(
      wrapSdkError({ errors: { name: "That notebook name is unavailable" } })
    )
    const wrapper = mountNotebookPageView(nb)

    await editPageName(wrapper, "notebook-page-name", "Unavailable")

    expect(wrapper.text()).toContain("That notebook name is unavailable")
    expect(wrapper.get('[data-test="notebook-page-name"]').text()).toBe(
      "Unavailable"
    )
  })
})
