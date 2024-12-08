import { flushPromises } from "@vue/test-utils"
import { saveAs } from "file-saver"
import NotebookAssistantManagementDialog from "@/components/notebook/NotebookAssistantManagementDialog.vue"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"

vitest.mock("file-saver", () => ({ saveAs: vitest.fn() }))

describe("NotebookAssistantManagementDialog.vue", () => {
  let wrapper
  const notebook = makeMe.aNotebook.please()
  const mockedDump = vitest.fn()
  const mockedUpdateAiAssistant = vitest.fn()
  const originalCreateObjectURL = URL.createObjectURL
  const originalRevokeObjectURL = URL.revokeObjectURL

  beforeEach(() => {
    global.URL.createObjectURL = vitest.fn()
    global.URL.revokeObjectURL = vitest.fn()
    helper.managedApi.restNotebookController.downloadNotebookDump = mockedDump
    helper.managedApi.restNotebookController.updateAiAssistant =
      mockedUpdateAiAssistant
    wrapper = helper
      .component(NotebookAssistantManagementDialog)
      .withProps({
        notebook,
      })
      .mount()
  })

  afterEach(() => {
    vitest.clearAllMocks()
    global.URL.createObjectURL = originalCreateObjectURL
    global.URL.revokeObjectURL = originalRevokeObjectURL
  })

  describe("AI Instructions Form", () => {
    it("updates AI instructions when form is submitted", async () => {
      const instructions = "Please use simple English."
      await wrapper
        .find('input[name="additionalInstruction"]')
        .setValue(instructions)
      await wrapper.find("form").trigger("submit")

      expect(mockedUpdateAiAssistant).toHaveBeenCalledWith(
        notebook.id,
        instructions
      )
    })
  })

  describe("Assistant Creation and Download", () => {
    it("renders the buttons", () => {
      const buttons = wrapper.findAll("button")
      expect(buttons).toHaveLength(3) // Update, Create, and Download buttons
      expect(buttons[1].text()).toContain("Create Assistant For Notebook")
      expect(buttons[2].text()).toContain("Download Notebook Dump")
    })

    it("downloads notebook dump when button is clicked", async () => {
      const noteBriefs = [{ id: 1, title: "Note 1", details: "Test content" }]
      mockedDump.mockResolvedValue(noteBriefs)

      await wrapper.findAll("button")[2].trigger("click")
      await flushPromises()

      expect(saveAs).toHaveBeenCalledWith(
        new Blob([JSON.stringify(noteBriefs, null, 2)], {
          type: "application/json",
        }),
        "notebook-dump.json"
      )
    })
  })
})
