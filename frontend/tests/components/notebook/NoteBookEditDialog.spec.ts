import type { Notebook } from "@/generated/backend"
import NotebookEditDialog from "@/components/notebook/NotebookEditDialog.vue"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"

describe("NotebookEditDialog.spec", () => {
  const notebook: Notebook = {
    ...makeMe.aNotebook.please(),
  }
  const wrapper = helper
    .component(NotebookEditDialog)
    .withRouter()
    .withProps({ notebook })
    .mount()

  it("Renders the default certificate expiry", async () => {
    expect(wrapper.find("[name='certificateExpiry']").exists()).toBe(true)
    expect(
      (wrapper.find("[name='certificateExpiry']").element as HTMLInputElement)
        .value
    ).toBe("1y")
  })
  it("The certificate expiry field is editable", async () => {
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
