import NotebookPageView from "@/pages/NotebookPageView.vue"
import helper from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { describe, it, expect } from "vitest"
import {
  aNotebook,
  mountNotebookPageView,
  noopFetchNotebookPage,
  stubNotebookPageViewBookAbsent,
} from "./notebookPageViewTestSupport"

describe("NotebookPageView.spec", () => {
  stubNotebookPageViewBookAbsent()

  const notebook = aNotebook()

  it("shows notebook name in title hero without catalog description", async () => {
    const nb = aNotebook({
      name: "My Notebook Title",
      description: "A short message for the notebook.",
    })
    const wrapper = mountNotebookPageView(nb)

    const summary = wrapper.find('[data-testid="notebook-page-summary"]')
    expect(summary.exists()).toBe(true)
    expect(
      wrapper.find('[data-testid="notebook-page-kind-label"]').text()
    ).toContain("Notebook")
    expect(summary.text()).toContain("My Notebook Title")
    expect(summary.text()).not.toContain("A short message for the notebook.")
    expect(summary.text()).not.toContain("Head note")
    expect(summary.find("a").exists()).toBe(false)
  })

  it("shows home landmarks and hides admin sections on first paint", async () => {
    const nb = aNotebook({
      name: "Notebook Home NB",
      description: "Home cue",
    })
    const wrapper = helper
      .component(NotebookPageView)
      .withRouter()
      .withProps({
        notebook: nb,
        fetchNotebookPage: noopFetchNotebookPage,
        readmeContent: "Readme canvas body",
      })
      .mount()

    expect(
      wrapper.find('[data-testid="notebook-page-kind-label"]').text()
    ).toContain("Notebook")
    expect(
      wrapper.find('[data-testid="notebook-page-summary"]').text()
    ).toContain("Notebook Home NB")
    expect(
      wrapper.find('[data-testid="notebook-page-summary"]').text()
    ).not.toContain("Home cue")
    expect(
      wrapper.find('[data-testid="notebook-readme"]').exists()
    ).toBe(true)
    expect(
      wrapper.find('[data-testid="notebook-readme-editor"]').exists()
    ).toBe(true)
    expect(
      wrapper
        .find('[data-testid="notebook-readme-body"]')
        .classes()
        .includes("scoped-readme-editor--flush")
    ).toBe(true)
    expect(
      wrapper.find('[data-testid="notebook-settings"]').exists()
    ).toBe(false)
    expect(
      wrapper.find('[data-testid="notebook-health"]').exists()
    ).toBe(false)
    expect(
      wrapper.find('[data-testid="notebook-tab-health"]').exists()
    ).toBe(true)
    expect(wrapper.text()).not.toContain("Notebook Management")
    expect(wrapper.text()).not.toContain("Notebook Settings")
    expect(wrapper.text()).not.toContain("Notebook Indexing")
    expect(wrapper.text()).not.toContain("Share notebook to bazaar")
    expect(wrapper.text()).not.toContain("Skip Memory Tracking")
    expect(wrapper.text()).not.toContain("Update index")
    expect(wrapper.text()).not.toContain("Reset notebook index")
  })

  it("shows Health panel and hides Settings after opening Health tab", async () => {
    const wrapper = mountNotebookPageView(notebook)

    expect(
      wrapper.find('[data-testid="notebook-health"]').exists()
    ).toBe(false)
    expect(
      wrapper.find('[data-testid="notebook-settings"]').exists()
    ).toBe(false)

    await wrapper
      .get('[data-testid="notebook-tab-health"]')
      .trigger("click")
    await flushPromises()

    expect(
      wrapper.find('[data-testid="notebook-health"]').exists()
    ).toBe(true)
    expect(
      wrapper.find('[data-testid="notebook-settings"]').exists()
    ).toBe(false)
    expect(
      wrapper.find('[data-testid="notebook-readme"]').exists()
    ).toBe(false)
  })

  it("shows admin sections only after opening Settings tab", async () => {
    const wrapper = mountNotebookPageView(notebook)

    expect(
      wrapper.find('[data-testid="notebook-settings"]').exists()
    ).toBe(false)
    expect(
      wrapper.find('[data-testid="notebook-readme-editor"]').exists()
    ).toBe(true)

    await wrapper
      .get('[data-testid="notebook-tab-settings"]')
      .trigger("click")
    await flushPromises()

    const settings = wrapper.find('[data-testid="notebook-settings"]')
    expect(settings.exists()).toBe(true)
    expect(settings.text()).toContain("Description")
    expect(settings.text()).toContain("Notebook Management")
    expect(settings.text()).toContain("Notebook Indexing")
    expect(settings.text()).toContain("Share notebook to bazaar")
    expect(settings.text()).toContain("Skip Memory Tracking")
    expect(settings.text()).toContain("Update index")
    expect(settings.text()).toContain("Reset notebook index")
    expect(settings.text()).not.toContain("Notebook Settings")
    expect(settings.text()).not.toContain("Update Settings")
    expect(
      wrapper.find('[data-testid="notebook-readme"]').exists()
    ).toBe(false)
    expect(
      wrapper.find('[data-testid="notebook-readme-editor"]').exists()
    ).toBe(false)
  })

  it("shows no-book copy without Read when getBook has no book", async () => {
    const wrapper = mountNotebookPageView(notebook)
    await wrapper
      .get('[data-testid="notebook-tab-settings"]')
      .trigger("click")
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
