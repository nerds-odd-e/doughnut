import { flushPromises } from "@vue/test-utils"
import NotebookAssistantManagementDialog from "@/components/notebook/NotebookAssistantManagementDialog.vue"
import makeMe from "@tests/fixtures/makeMe"
import helper, { mockSdkServiceWithImplementation } from "@tests/helpers"
import { describe, it, expect, beforeEach, afterEach, vi } from "vitest"

describe("NotebookAssistantManagementDialog.vue", () => {
  let wrapper
  const notebook = makeMe.aNotebook.please()
  const mockedUpdateAiAssistant = vi.fn()

  beforeEach(() => {
    mockSdkServiceWithImplementation("updateAiAssistant", async (options) => {
      return await mockedUpdateAiAssistant(options)
    })
    wrapper = helper
      .component(NotebookAssistantManagementDialog)
      .withProps({
        notebook,
        additionalInstructions: "",
      })
      .mount()
  })

  afterEach(() => {
    vi.clearAllMocks()
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
