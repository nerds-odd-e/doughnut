import { flushPromises } from "@vue/test-utils"
import { saveAs } from "file-saver"
import NotebookAssistantManagementDialog from "@/components/notebook/NotebookAssistantManagementDialog.vue"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"
import type { NotebookAiAssistant } from "@generated/backend"

vitest.mock("file-saver", () => ({ saveAs: vitest.fn() }))

describe("NotebookAssistantManagementDialog.vue", () => {
  let wrapper
  const notebook = makeMe.aNotebook.please()
  const mockedDump = vitest.fn()
  const mockedUpdateAiAssistant = vitest.fn()
  const mockedGetAiAssistant = vitest.fn()
  const originalCreateObjectURL = URL.createObjectURL
  const originalRevokeObjectURL = URL.revokeObjectURL

  beforeEach(() => {
    global.URL.createObjectURL = vitest.fn()
    global.URL.revokeObjectURL = vitest.fn()
    helper.managedApi.restNotebookController.downloadNotebookDump = mockedDump
    helper.managedApi.restNotebookController.updateAiAssistant =
      mockedUpdateAiAssistant
    helper.managedApi.restNotebookController.getAiAssistant =
      mockedGetAiAssistant
    mockedGetAiAssistant.mockResolvedValue(null) // default to no existing settings
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

      expect(mockedUpdateAiAssistant).toHaveBeenCalledWith(notebook.id, {
        additionalInstructions: instructions,
      })
    })
  })

  describe("Download", () => {
    it("renders the download button", () => {
      const buttons = wrapper.findAll("button")
      expect(buttons).toHaveLength(2) // Update and Download buttons
      expect(buttons[1].text()).toContain("Download Notebook Dump")
    })

    it("downloads notebook dump when button is clicked", async () => {
      const noteBriefs = [{ id: 1, title: "Note 1", details: "Test content" }]
      mockedDump.mockResolvedValue(noteBriefs)

      await wrapper.findAll("button")[1].trigger("click")
      await flushPromises()

      expect(saveAs).toHaveBeenCalledWith(
        new Blob([JSON.stringify(noteBriefs, null, 2)], {
          type: "application/json",
        }),
        "notebook-dump.json"
      )
    })
  })

  describe("Component Initialization", () => {
    it("loads empty settings when no assistant exists", async () => {
      mockedGetAiAssistant.mockResolvedValue(null)
      wrapper = helper
        .component(NotebookAssistantManagementDialog)
        .withProps({ notebook })
        .mount()
      await flushPromises()

      const input = wrapper.find('input[name="additionalInstruction"]')
      expect(input.element.value).toBe("")
    })

    it("loads existing settings when assistant exists", async () => {
      const existingInstructions = "Existing instructions"
      const mockAssistant: Partial<NotebookAiAssistant> = {
        additionalInstructionsToAi: existingInstructions,
        id: 1,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      }
      mockedGetAiAssistant.mockResolvedValue(mockAssistant)

      wrapper = helper
        .component(NotebookAssistantManagementDialog)
        .withProps({ notebook })
        .mount()
      await flushPromises()

      const input = wrapper.find('input[name="additionalInstruction"]')
      expect(input.element.value).toBe(existingInstructions)
    })
  })
})
