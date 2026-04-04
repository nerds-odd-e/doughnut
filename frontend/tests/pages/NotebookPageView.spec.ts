import type { Notebook } from "@generated/doughnut-backend-api"
import { NotebookBooksController } from "@generated/doughnut-backend-api/sdk.gen"
import NotebookPageView from "@/pages/NotebookPageView.vue"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService, wrapSdkError } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, it, expect, vi } from "vitest"

describe("NotebookPageView.spec", () => {
  beforeEach(() => {
    mockSdkService("getApprovalForNotebook", { approval: undefined })
    vi.spyOn(NotebookBooksController, "getBook").mockResolvedValue(
      wrapSdkError("Not found") as Awaited<
        ReturnType<typeof NotebookBooksController.getBook>
      >
    )
  })

  const notebook: Notebook = {
    ...makeMe.aNotebook.please(),
  }

  it("Renders the default certificate expiry", async () => {
    const wrapper = helper
      .component(NotebookPageView)
      .withRouter()
      .withProps({ notebook })
      .mount()
    expect(wrapper.find("[name='certificateExpiry']").exists()).toBe(true)
    expect(
      (wrapper.find("[name='certificateExpiry']").element as HTMLInputElement)
        .value
    ).toBe("1y")
  })
  it("The certificate expiry field is editable", async () => {
    const wrapper = helper
      .component(NotebookPageView)
      .withRouter()
      .withProps({ notebook })
      .mount()
    expect(
      (wrapper.find("[name='certificateExpiry']").element as HTMLInputElement)
        .value
    ).toBe("1y")
    wrapper.find("[name='certificateExpiry']").setValue("2y 3m 4w 5d")
    expect(
      (wrapper.find("[name='certificateExpiry']").element as HTMLInputElement)
        .value
    ).toBe("2y 3m 4w 5d")
  })

  it("shows no-book copy without Read when getBook has no book", async () => {
    const wrapper = helper
      .component(NotebookPageView)
      .withRouter()
      .withProps({ notebook })
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
