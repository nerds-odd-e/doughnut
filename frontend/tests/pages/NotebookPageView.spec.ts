import type { Notebook } from "@generated/backend"
import NotebookPageView from "@/pages/NotebookPageView.vue"
import makeMe from "@tests/fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import { beforeEach } from "vitest"

describe("NotebookPageView.spec", () => {
  beforeEach(() => {
    mockSdkService("getApprovalForNotebook", { approval: undefined })
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
})
