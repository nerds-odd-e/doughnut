import AttachmentPage from "@/pages/AttachmentPage.vue"
import type { Notebook } from "@generated/donut-backend-api"
import helper from "@tests/helpers"
import makeMe from "donut-test-fixtures/makeMe"
import { describe, expect, it } from "vitest"

const mountPage = ({
  notebook = makeMe.aNotebook.please(),
  readonly = true,
  filename = "run.json",
  picture = false,
}: {
  notebook?: Notebook
  readonly?: boolean
  filename?: string
  picture?: boolean
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
        picture,
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

  it("shows a picture file's picture and keeps its download link", () => {
    const notebook = makeMe.aNotebook.please()
    const wrapper = mountPage({ notebook, filename: "flow.png", picture: true })

    const img = wrapper.get<HTMLImageElement>("img")
    expect(new URL(img.element.src).pathname).toBe(
      `/api/notebooks/${notebook.id}/attachments/42/picture`
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
    "beside a picture, when readonly is %s, offers Delete: %s",
    (readonly, offered) => {
      const wrapper = mountPage({
        readonly,
        filename: "flow.png",
        picture: true,
      })

      expect(
        wrapper.find('[data-testid="attachment-delete-button"]').exists()
      ).toBe(offered)
    }
  )

  it("shows no picture for a file that is not a picture", () => {
    expect(mountPage().find("img").exists()).toBe(false)
  })
})
