import AttachmentPage from "@/pages/AttachmentPage.vue"
import type { Notebook, NoteTopology } from "@generated/donut-backend-api"
import helper from "@tests/helpers"
import makeMe from "donut-test-fixtures/makeMe"
import { describe, expect, it } from "vitest"

const mountPage = ({
  notebook = makeMe.aNotebook.please(),
  readonly = true,
  filename = "run.json",
  image = false,
  references = [],
}: {
  notebook?: Notebook
  readonly?: boolean
  filename?: string
  image?: boolean
  references?: NoteTopology[]
} = {}) =>
  helper
    .component(AttachmentPage)
    .withRouter()
    .withProps({
      attachmentRealm: {
        notebookRealm: { notebook, readonly },
        ancestorFolders: [],
        attachment: { id: 42, filename },
        size: 12,
        image,
        references,
      },
    })
    .mount()

describe("AttachmentPage", () => {
  it("shows the filename, its size in bytes, and a download link for its content", () => {
    const notebook = makeMe.aNotebook.please()
    const wrapper = mountPage({ notebook })

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

  it("shows an image file and keeps its download link", () => {
    const notebook = makeMe.aNotebook.please()
    const wrapper = mountPage({ notebook, filename: "flow.png", image: true })

    const img = wrapper.get<HTMLImageElement>("img")
    expect(new URL(img.element.src).pathname).toBe(
      `/api/notebooks/${notebook.id}/attachments/42/image`
    )
    expect(img.attributes("alt")).toBe("flow.png")
    expect(
      wrapper.find('[data-testid="attachment-download-link"]').exists()
    ).toBe(true)
  })

  it.each([
    [true, false],
    [false, true],
  ])(
    "beside an image, when readonly is %s, offers Delete: %s",
    (readonly, offered) => {
      const wrapper = mountPage({
        readonly,
        filename: "flow.png",
        image: true,
      })

      expect(
        wrapper.find('[data-testid="attachment-delete-button"]').exists()
      ).toBe(offered)
    }
  )

  it("shows no image for a file that is not an image", () => {
    expect(mountPage().find("img").exists()).toBe(false)
  })

  it("shows the References heading and both titles when there are references", () => {
    const design = makeMe.aNote.title("Design").please().noteTopology
    const review = makeMe.aNote.title("Review").please().noteTopology
    const wrapper = mountPage({ references: [design, review] })

    expect(wrapper.get("h3").text()).toBe("References")
    expect(wrapper.text()).toContain("Design")
    expect(wrapper.text()).toContain("Review")
  })

  it("shows no References heading when there are no references", () => {
    const wrapper = mountPage({ references: [] })

    expect(wrapper.find("h3").exists()).toBe(false)
  })
})
