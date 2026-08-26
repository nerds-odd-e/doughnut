import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import NotebookNewForm from "@/components/notebook/NotebookNewForm.vue"
import { beforeEach, describe, expect, it } from "vitest"
import makeMe from "donut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"

describe("NotebookNewForm with notebook group", () => {
  const createdNotebook = makeMe.aNotebook.id(42).please()

  beforeEach(() => {
    mockSdkService(NotebookController, "createNotebook", {
      notebook: createdNotebook,
      readonly: false,
    })
  })

  it("shows which group the notebook will be created in", () => {
    const wrapper = helper
      .component(NotebookNewForm)
      .withProps({
        notebookGroup: { id: 7, name: "Study" },
      })
      .withRouter()
      .mount()

    expect(
      wrapper.find('[data-testid="notebook-new-form-group-hint"]').text()
    ).toBe('Creates in group "Study".')
  })

  it("submits notebookGroupId when creating into a group", async () => {
    const createSpy = mockSdkService(NotebookController, "createNotebook", {
      notebook: createdNotebook,
      readonly: false,
    })

    const wrapper = helper
      .component(NotebookNewForm)
      .withProps({
        notebookGroup: { id: 7, name: "Study" },
      })
      .withRouter()
      .mount()

    const vm = wrapper.vm as unknown as {
      noteFormData: { newTitle: string; description: string }
    }
    vm.noteFormData.newTitle = "In Study"
    await wrapper.find("form").trigger("submit")
    await flushPromises()

    expect(createSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        body: expect.objectContaining({
          newTitle: "In Study",
          notebookGroupId: 7,
        }),
      })
    )
  })
})
