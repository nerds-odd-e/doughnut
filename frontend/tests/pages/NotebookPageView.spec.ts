import type { Notebook } from "@generated/doughnut-backend-api"
import {
  NotebookBooksController,
  NotebookController,
} from "@generated/doughnut-backend-api/sdk.gen"
import NotebookPageView from "@/pages/NotebookPageView.vue"
import SeamlessTextEditor from "@/components/form/SeamlessTextEditor.vue"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, {
  mockSdkService,
  wrapSdkError,
  wrapSdkResponse,
} from "@tests/helpers"
import usePopups from "@/components/commons/Popups/usePopups"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, it, expect, vi } from "vitest"

describe("NotebookPageView.spec", () => {
  beforeEach(() => {
    mockSdkService(NotebookController, "getAiAssistant", {
      id: 1,
      additionalInstructionsToAi: "",
    })
    vi.spyOn(NotebookBooksController, "getBook").mockResolvedValue(
      wrapSdkError("Not found") as Awaited<
        ReturnType<typeof NotebookBooksController.getBook>
      >
    )
  })

  const notebook: Notebook = {
    ...makeMe.aNotebook.please(),
  }

  const noopFetchNotebookPage = async (): Promise<void> => {
    await Promise.resolve()
  }

  it("shows notebook name and description in summary with no link row in the summary", async () => {
    const nb: Notebook = {
      ...makeMe.aNotebook.please(),
      name: "My Notebook Title",
      description: "A short message for the notebook.",
    }
    const wrapper = helper
      .component(NotebookPageView)
      .withRouter()
      .withProps({ notebook: nb, fetchNotebookPage: noopFetchNotebookPage })
      .mount()

    const summary = wrapper.find('[data-testid="notebook-page-summary"]')
    expect(summary.exists()).toBe(true)
    expect(summary.text()).toContain("My Notebook Title")
    expect(summary.text()).toContain("A short message for the notebook.")
    expect(summary.text()).not.toContain("Head note")
    expect(summary.find("a").exists()).toBe(false)
  })

  it("shows notebook name in summary without description block when description is absent", async () => {
    const nb: Notebook = {
      ...makeMe.aNotebook.please(),
      name: "Title Only NB",
      description: undefined,
    }
    const wrapper = helper
      .component(NotebookPageView)
      .withRouter()
      .withProps({ notebook: nb, fetchNotebookPage: noopFetchNotebookPage })
      .mount()

    const summary = wrapper.find('[data-testid="notebook-page-summary"]')
    expect(summary.text()).toContain("Title Only NB")
    expect(summary.find(".notebook-page-summary-description").exists()).toBe(
      false
    )
  })

  it("sends description when saving notebook settings", async () => {
    const nb: Notebook = {
      ...makeMe.aNotebook.please(),
      description: "Initial blurb",
    }
    const updateSpy = vi
      .spyOn(NotebookController, "updateNotebook")
      .mockResolvedValue(wrapSdkResponse({ ...nb, description: "Saved blurb" }))
    const wrapper = helper
      .component(NotebookPageView)
      .withRouter()
      .withProps({ notebook: nb, fetchNotebookPage: noopFetchNotebookPage })
      .mount()

    await wrapper.find("[name='description']").setValue("Saved blurb")
    await wrapper.find("button.daisy-btn-primary.daisy-mt-4").trigger("click")
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

  it("sends current settings and new name when updating notebook name from summary", async () => {
    const nb: Notebook = {
      ...makeMe.aNotebook.please(),
      name: "Original title",
    }
    const updateSpy = vi
      .spyOn(NotebookController, "updateNotebook")
      .mockResolvedValue(wrapSdkResponse({ ...nb, name: "Edited title" }))
    const wrapper = helper
      .component(NotebookPageView)
      .withRouter()
      .withProps({ notebook: nb, fetchNotebookPage: noopFetchNotebookPage })
      .mount()

    await wrapper
      .get('[data-testid="notebook-page-name-edit"]')
      .trigger("click")
    wrapper
      .findComponent(SeamlessTextEditor)
      .vm.$emit("update:modelValue", "Edited title")
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

  it("shows no-book copy without Read when getBook has no book", async () => {
    const wrapper = helper
      .component(NotebookPageView)
      .withRouter()
      .withProps({ notebook, fetchNotebookPage: noopFetchNotebookPage })
      .mount()
    await flushPromises()

    const empty = wrapper.find('[data-testid="notebook-no-book"]')
    expect(empty.exists()).toBe(true)
    expect(empty.text()).toContain("No book attached to this notebook.")
    expect(
      wrapper.find('[data-testid="notebook-attached-book"]').exists()
    ).toBe(false)
    const readButtons = wrapper
      .findAll("button")
      .filter((b) => b.text() === "Read")
    expect(readButtons.length).toBe(0)
  })
})
