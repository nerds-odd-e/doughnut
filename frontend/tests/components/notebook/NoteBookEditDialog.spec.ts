import { Notebook } from "@/generated/backend"
import { flushPromises } from "@vue/test-utils"
import NotebookEditDialog from "@/components/notebook/NotebookEditDialog.vue"
import makeMe from "../../fixtures/makeMe"
import helper from "../../helpers"

describe("NotebookEditDialog.spec", () => {
  const notebook: Notebook = {
    ...makeMe.aNotebook.please(),
  }
  const wrapper = helper
    .component(NotebookEditDialog)
    .withProps({ notebook })
    .mount()

  it("Renders the default expiry", async () => {
    await flushPromises()
    expect(wrapper.find("[name='certificateExpiry']").exists()).toBe(true)
    expect(
      (wrapper.find("[name='certificateExpiry']").element as HTMLInputElement)
        .value
    ).toBe("1y")
  })
})
