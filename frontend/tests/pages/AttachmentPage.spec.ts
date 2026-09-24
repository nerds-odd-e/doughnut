import AttachmentPage from "@/pages/AttachmentPage.vue"
import helper from "@tests/helpers"
import makeMe from "donut-test-fixtures/makeMe"
import { describe, expect, it } from "vitest"

describe("AttachmentPage", () => {
  it("shows the filename, its size in bytes, and a download link for its content", () => {
    const notebook = makeMe.aNotebook.please()
    const wrapper = helper
      .component(AttachmentPage)
      .withProps({
        attachmentRealm: {
          notebookRealm: { notebook, readonly: true },
          ancestorFolders: [],
          attachment: { id: 42, filename: "run.json" },
          size: 12,
        },
      })
      .mount()

    expect(wrapper.get('[data-testid="attachment-page-filename"]').text()).toBe(
      "run.json"
    )
    expect(wrapper.get('[data-testid="attachment-page-size"]').text()).toBe(
      "12 bytes"
    )
    const link = wrapper.get<HTMLAnchorElement>(
      '[data-testid="attachment-download-link"]'
    )
    expect(link.element.pathname).toBe(
      `/api/notebooks/${notebook.id}/attachments/42/content`
    )
    expect(link.attributes("download")).toBe("run.json")
  })
})
