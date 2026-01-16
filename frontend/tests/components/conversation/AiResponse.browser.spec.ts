import AiResponse from "@/components/conversations/AiResponse.vue"
import createNoteStorage from "@/store/createNoteStorage"
import type { TitleReplacement } from "@generated/backend"
import makeMe from "@tests/fixtures/makeMe"
import helper, { mockSdkService, mockShowNote } from "@tests/helpers"
import {
  getLastInstance,
  resetInstance,
} from "@tests/helpers/aiReplyEventSourceTracker"
import { flushPromises } from "@vue/test-utils"
import { expect, vi } from "vitest"

// Browser Mode: No IntersectionObserver mock needed - real API available!
// Browser Mode: No window.performance mock needed - real API available!

// Mock AiReplyEventSource to track instances (module mock - still needed)
vi.mock("@/managedApi/AiReplyEventSource", async () => {
  const actual = await vi.importActual<
    typeof import("@/managedApi/AiReplyEventSource")
  >("@/managedApi/AiReplyEventSource")
  const { setLastInstance } = await import(
    "@tests/helpers/aiReplyEventSourceTracker"
  )
  return {
    default: class extends actual.default {
      constructor(conversationId: number) {
        super(conversationId)
        setLastInstance(this)
      }
    },
  }
})

export const simulateAiResponse = (content = "## I'm ChatGPT") => {
  const instance = getLastInstance()
  if (!instance) {
    throw new Error("No AiReplyEventSource instance available")
  }
  const chunk = {
    choices: [
      {
        index: 0,
        message: {
          role: "assistant",
          content,
        },
        finish_reason: null,
      },
    ],
  }

  instance.onMessageCallback("chat.completion.chunk", JSON.stringify(chunk))
}

// Create realistic streaming tool call chunks (delta.tool_calls format)
const createToolCallChunk = (functionName: string, args: object) => {
  const argumentsString = JSON.stringify(args)
  // Return array of chunks to simulate realistic streaming
  return [
    // First chunk: tool call with id, name, and start of arguments
    {
      choices: [
        {
          index: 0,
          delta: {
            role: "assistant",
            tool_calls: [
              {
                index: 0,
                id: "call-456",
                type: "function",
                function: {
                  name: functionName,
                  arguments: argumentsString.substring(
                    0,
                    Math.min(20, argumentsString.length)
                  ),
                },
              },
            ],
          },
          finish_reason: null,
        },
      ],
    },
    // Subsequent chunks: continue with fragmented arguments (if needed)
    ...(argumentsString.length > 20
      ? [
          {
            choices: [
              {
                index: 0,
                delta: {
                  tool_calls: [
                    {
                      index: 0,
                      function: {
                        arguments: argumentsString.substring(20),
                      },
                    },
                  ],
                },
                finish_reason: null,
              },
            ],
          },
        ]
      : []),
    // Final chunk: finish_reason triggers processing
    {
      choices: [
        {
          index: 0,
          delta: {},
          finish_reason: "tool_calls",
        },
      ],
    },
  ]
}

const setupTestData = () => {
  const noteRealm = makeMe.aNoteRealm.please()
  const note = noteRealm.note
  const conversation = makeMe.aConversation.forANote(note).please()
  return { note, noteRealm, conversation }
}

const mountComponent = (conversation) => {
  return helper
    .component(AiResponse)
    .withProps({ conversation, aiReplyTrigger: 0 })
    .mount()
}

const submitMessage = async (wrapper) => {
  await wrapper.vm.getAiReply()
  await flushPromises()
}

const submitMessageAndSimulateToolCallChunk = async (
  wrapper,
  toolCallChunks
) => {
  await submitMessage(wrapper)
  const instance = getLastInstance()
  if (!instance) {
    throw new Error("No AiReplyEventSource instance available")
  }
  // toolCallChunks can be a single chunk (old format) or array of chunks (new format)
  const chunks = Array.isArray(toolCallChunks)
    ? toolCallChunks
    : [toolCallChunks]
  for (const chunk of chunks) {
    instance.onMessageCallback("chat.completion.chunk", JSON.stringify(chunk))
    await flushPromises()
  }
}

const submitMessageAndSimulateRunResponse = async (wrapper, toolCallChunk) => {
  // Alias for submitMessageAndSimulateToolCallChunk for backward compatibility
  await submitMessageAndSimulateToolCallChunk(wrapper, toolCallChunk)
}

import { useStorageAccessor } from "@/composables/useStorageAccessor"

describe("AiResponse", () => {
  let wrapper
  let note
  let noteRealm
  let conversation
  const storageAccessor = useStorageAccessor()

  beforeEach(() => {
    storageAccessor.value = createNoteStorage()
    resetInstance()
    mockShowNote()

    const testData = setupTestData()
    note = testData.note
    noteRealm = testData.noteRealm
    conversation = testData.conversation

    wrapper = mountComponent(conversation)
  })

  afterEach(() => {
    resetInstance()
  })

  describe("AI Reply", () => {
    beforeEach(async () => {
      await submitMessage(wrapper)
    })

    it("processes AI response and displays content", async () => {
      simulateAiResponse()
      await flushPromises()

      expect(wrapper.find(".ai-chat h2").text()).toEqual("I'm ChatGPT")
    })

    describe("Status Bar", () => {
      it("shows correct status messages during AI reply lifecycle", async () => {
        const statusBar = wrapper.find(".status-bar")
        const statusText = () => statusBar.find("small").text()

        // Initial state
        expect(statusBar.exists()).toBe(true)
        expect(statusText()).toBe("Starting AI reply...")

        // During writing
        const instance = getLastInstance()
        if (!instance) {
          throw new Error("No AiReplyEventSource instance available")
        }
        instance.onMessageCallback(
          "chat.completion.chunk",
          JSON.stringify({
            choices: [
              {
                index: 0,
                message: { role: "assistant", content: "Test" },
                finish_reason: null,
              },
            ],
          })
        )
        await wrapper.vm.$nextTick()
        expect(statusText()).toBe("Streaming response...")

        // After completion
        instance.onMessageCallback("done", "")
        await wrapper.vm.$nextTick()
        expect(wrapper.find(".status-bar").exists()).toBe(false)
      })

      it("hides status bar and shows error message on failure", async () => {
        const instance = getLastInstance()
        if (!instance) {
          throw new Error("No AiReplyEventSource instance available")
        }
        const onError = instance.onErrorCallback
        if (!onError) throw new Error("onError is not defined")

        onError(new Error("400 Bad Request"))
        await wrapper.vm.$nextTick()

        expect(wrapper.find(".status-bar").exists()).toBe(false)
        expect(wrapper.find(".last-error-message").text()).toBe("Bad Request")
      })
    })
  })

  describe("Tool Call Handling", () => {
    const testDetails = "**bold completion**"
    let updateNoteDetailsSpy: ReturnType<
      typeof mockSdkService<"updateNoteDetails">
    >

    beforeEach(async () => {
      updateNoteDetailsSpy = mockSdkService(
        "updateNoteDetails",
        makeMe.aNoteRealm.please()
      )

      await submitMessageAndSimulateRunResponse(
        wrapper,
        createToolCallChunk("NoteDetailsCompletion", {
          details: testDetails,
        })
      )
    })

    it("formats completion suggestion correctly based on existing content", async () => {
      // Test empty note details
      noteRealm.note.details = ""
      storageAccessor.value.refreshNoteRealm(noteRealm)
      const emptyDetails = "**bold completion**"
      await submitMessageAndSimulateRunResponse(
        wrapper,
        createToolCallChunk("NoteDetailsCompletion", {
          details: emptyDetails,
        })
      )
      // Markdown is rendered to HTML, so check for the rendered content
      expect(wrapper.find(".completion-text").html()).toContain(
        "<strong>bold completion</strong>"
      )

      // Test with existing note details
      noteRealm.note.details = "Existing content"
      storageAccessor.value.refreshNoteRealm(noteRealm)
      const newDetails = "Existing content\n**bold completion**"
      await submitMessageAndSimulateRunResponse(
        wrapper,
        createToolCallChunk("NoteDetailsCompletion", {
          details: newDetails,
        })
      )
      // Markdown is rendered to HTML, so check for the rendered content
      expect(wrapper.find(".completion-text").html()).toContain(
        "Existing content"
      )
      expect(wrapper.find(".completion-text").html()).toContain(
        "<strong>bold completion</strong>"
      )
    })

    it("formats completion suggestion with details", async () => {
      noteRealm.note.details = "Hello world"
      storageAccessor.value.refreshNoteRealm(noteRealm)
      const details = "Hello  friends!"
      await submitMessageAndSimulateRunResponse(
        wrapper,
        createToolCallChunk("NoteDetailsCompletion", {
          details,
        })
      )

      // The details should be displayed (markdown renders double spaces as single)
      expect(wrapper.find(".completion-text").html()).toContain(
        "Hello friends!"
      )
    })

    it("handles replacement when replacing all content", async () => {
      noteRealm.note.details = "Short\ntext"
      storageAccessor.value.refreshNoteRealm(noteRealm)
      const details = "New content"
      await submitMessageAndSimulateRunResponse(
        wrapper,
        createToolCallChunk("NoteDetailsCompletion", {
          details,
        })
      )

      // The details should show the replacement
      expect(wrapper.find(".completion-text").html()).toContain("New content")
    })

    it("accepts the completion suggestion and updates the note", async () => {
      noteRealm.note.details = ""
      storageAccessor.value.refreshNoteRealm(noteRealm)
      // Accept the suggestion
      await wrapper.find('button[class*="btn-primary"]').trigger("click")
      await flushPromises()

      // The details should replace the content
      expect(updateNoteDetailsSpy).toHaveBeenCalledWith({
        path: { note: note.id },
        body: { details: "**bold completion**" },
      })

      // Tool calls are executed inline with Chat Completion API
      // No need to submit results

      expect(wrapper.find(".completion-text").exists()).toBe(false)
    })

    it("cancel the completion suggestion without updating the note", async () => {
      // Clear any previous calls from setup
      vi.clearAllMocks()

      // Cancel the suggestion
      await wrapper.find('button[class*="btn-secondary"]').trigger("click")
      await flushPromises()

      expect(updateNoteDetailsSpy).not.toHaveBeenCalled()

      // Rejection is handled silently - no API calls needed

      expect(wrapper.find(".completion-text").exists()).toBe(false)
    })

    it("skips the completion suggestion without updating the note", async () => {
      // Clear any previous calls from setup
      vi.clearAllMocks()

      // Skip the suggestion
      await wrapper
        .find('button[class*="btn-outline-secondary"]')
        .trigger("click")
      await flushPromises()

      expect(updateNoteDetailsSpy).not.toHaveBeenCalled()

      // Tool calls are executed inline with Chat Completion API
      // No need to submit results

      expect(wrapper.find(".completion-text").exists()).toBe(false)
    })

    it("handles completion with replacement", async () => {
      noteRealm.note.details = "Hello world"
      storageAccessor.value.refreshNoteRealm(noteRealm)
      const details = "Hello  friends!"
      await submitMessageAndSimulateRunResponse(
        wrapper,
        createToolCallChunk("NoteDetailsCompletion", {
          details,
        })
      )

      // Check the formatted suggestion shows the details (markdown renders double spaces as single)
      expect(wrapper.find(".completion-text").html()).toContain(
        "Hello friends!"
      )
      // Accept the suggestion
      await wrapper.find('button[class*="btn-primary"]').trigger("click")
      await flushPromises()

      // Should replace with "Hello  friends!"
      expect(updateNoteDetailsSpy).toHaveBeenCalledWith({
        path: { note: note.id },
        body: { details: "Hello  friends!" },
      })
    })

    it("handles replacement by removing all content", async () => {
      noteRealm.note.details = "Hello world"
      storageAccessor.value.refreshNoteRealm(noteRealm)
      const details = "Completely new text"
      await submitMessageAndSimulateRunResponse(
        wrapper,
        createToolCallChunk("NoteDetailsCompletion", {
          details,
        })
      )

      // Accept the suggestion
      await wrapper.find('button[class*="btn-primary"]').trigger("click")
      await flushPromises()

      // Should replace everything with new text
      expect(updateNoteDetailsSpy).toHaveBeenCalledWith({
        path: { note: note.id },
        body: { details: "Completely new text" },
      })
    })

    describe("Note Access", () => {
      let updateNoteDetailsSpy: ReturnType<
        typeof mockSdkService<"updateNoteDetails">
      >

      beforeEach(async () => {
        updateNoteDetailsSpy = mockSdkService(
          "updateNoteDetails",
          makeMe.aNoteRealm.please()
        )
      })

      it("fails to handle completion when note is in answeredQuestion but not in subject", async () => {
        // Create conversation with note in answeredQuestion but not in subject
        const answeredQuestion = makeMe.anAnsweredQuestion
          .withNote(note)
          .please()
        const conversation = makeMe.aConversation
          .forAnsweredQuestion(answeredQuestion)
          .please()

        const wrapper = mountComponent(conversation)

        // Simulate completion suggestion
        const details = "test completion"
        await submitMessageAndSimulateRunResponse(
          wrapper,
          createToolCallChunk("NoteDetailsCompletion", {
            details,
          })
        )

        // Try to accept the completion
        await wrapper.find('button[class*="btn-primary"]').trigger("click")
        await flushPromises()

        expect(updateNoteDetailsSpy).toHaveBeenCalled()
      })
    })
  })

  describe("Title Title Generation", () => {
    const testTitle = "Generated Title"
    let updateNoteTitleSpy: ReturnType<typeof mockSdkService<"updateNoteTitle">>

    beforeEach(async () => {
      updateNoteTitleSpy = mockSdkService(
        "updateNoteTitle",
        makeMe.aNoteRealm.please()
      )

      await submitMessageAndSimulateRunResponse(
        wrapper,
        createToolCallChunk("TitleReplacement", <TitleReplacement>{
          newTitle: testTitle,
        })
      )
    })

    it("accepts the title suggestion and updates the note", async () => {
      await wrapper.find('button[class*="btn-primary"]').trigger("click")
      await flushPromises()

      expect(updateNoteTitleSpy).toHaveBeenCalledWith({
        path: { note: note.id },
        body: { newTitle: testTitle },
      })

      // Tool calls are executed inline with Chat Completion API
      // No need to submit results

      expect(wrapper.find(".title-suggestion").exists()).toBe(false)
    })

    it("rejects the title suggestion without updating the note", async () => {
      // Clear any previous calls from setup
      vi.clearAllMocks()

      await wrapper.find('button[class*="btn-secondary"]').trigger("click")
      await flushPromises()

      expect(updateNoteTitleSpy).not.toHaveBeenCalled()

      // Rejection is handled silently - no API calls needed

      expect(wrapper.find(".title-suggestion").exists()).toBe(false)
    })

    it("skips the title suggestion without updating the note", async () => {
      // Clear any previous calls from setup
      vi.clearAllMocks()

      await wrapper
        .find('button[class*="btn-outline-secondary"]')
        .trigger("click")
      await flushPromises()

      expect(updateNoteTitleSpy).not.toHaveBeenCalled()

      // Tool calls are executed inline with Chat Completion API
      // No need to submit results

      expect(wrapper.find(".title-suggestion").exists()).toBe(false)
    })
  })

  describe("Unknown Tool Call Handling", () => {
    const testJson = { unknown: "data" }

    beforeEach(async () => {
      const run = createToolCallChunk("unknown_tool", testJson)

      await submitMessageAndSimulateToolCallChunk(wrapper, run)
    })

    it("displays unknown tool call with raw JSON", () => {
      const unknownRequest = wrapper.find(".unknown-request")
      expect(unknownRequest.exists()).toBe(true)
      expect(unknownRequest.text()).toContain(JSON.stringify(testJson))

      const title = wrapper.find(".ai-chat")
      expect(title.text()).toContain("unknown_tool")
    })

    it("has accept button disabled", () => {
      const acceptButton = wrapper.find('button[class*="btn-primary"]')
      expect(acceptButton.exists()).toBe(false)
    })

    it("skips the unknown request", async () => {
      await wrapper
        .find('button[class*="btn-outline-secondary"]')
        .trigger("click")
      await flushPromises()

      // Tool calls are executed inline with Chat Completion API
      // No need to submit results

      expect(wrapper.find(".unknown-request").exists()).toBe(false)
    })

    it("cancels the unknown request", async () => {
      await wrapper.find('button[class*="btn-secondary"]').trigger("click")
      await flushPromises()

      // Rejection is handled silently - no API calls needed

      expect(wrapper.find(".unknown-request").exists()).toBe(false)
    })
  })
})
