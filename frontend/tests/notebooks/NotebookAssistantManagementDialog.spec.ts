import { flushPromises } from "@vue/test-utils"
import { saveAs } from "file-saver"
import NotebookAssistantManagementDialog from "@/components/notebook/NotebookAssistantManagementDialog.vue"
import makeMe from "@tests/fixtures/makeMe"
import helper, { mockSdkServiceWithImplementation } from "@tests/helpers"

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
    mockSdkServiceWithImplementation(
      "downloadNotebookDump",
      async (options) => {
        return await mockedDump(options)
      }
    )
    mockSdkServiceWithImplementation("updateAiAssistant", async (options) => {
      return await mockedUpdateAiAssistant(options)
    })
    mockedDump.mockResolvedValue([])
    wrapper = helper
      .component(NotebookAssistantManagementDialog)
      .withProps({
        notebook,
        additionalInstructions: "",
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
      mockedUpdateAiAssistant.mockResolvedValue(undefined)
      const instructions = "Please use simple English."
      await wrapper
        .find('input[name="additionalInstruction"]')
        .setValue(instructions)
      await wrapper.find("form").trigger("submit")
      await flushPromises()

      expect(mockedUpdateAiAssistant).toHaveBeenCalledWith({
        path: { notebook: notebook.id },
        body: {
          additionalInstructions: instructions,
        },
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
      wrapper = helper
        .component(NotebookAssistantManagementDialog)
        .withProps({ notebook, additionalInstructions: "" })
        .mount()
      await flushPromises()

      const input = wrapper.find('input[name="additionalInstruction"]')
      expect(input.element.value).toBe("")
    })

    it("loads existing settings when assistant exists", async () => {
      const existingInstructions = "Existing instructions"
      wrapper = helper
        .component(NotebookAssistantManagementDialog)
        .withProps({ notebook, additionalInstructions: existingInstructions })
        .mount()
      await flushPromises()

      const input = wrapper.find('input[name="additionalInstruction"]')
      expect(input.element.value).toBe(existingInstructions)
    })
  })
})
