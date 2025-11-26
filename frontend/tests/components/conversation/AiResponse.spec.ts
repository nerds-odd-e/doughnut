import AiResponse from "@/components/conversations/AiResponse.vue"
import { expect, vi } from "vitest"
import helper, { mockShowNote, mockSdkService } from "@tests/helpers"
import makeMe from "@tests/fixtures/makeMe"
import type { TitleReplacement } from "@generated/backend"
import { flushPromises } from "@vue/test-utils"
import createNoteStorage from "@/store/createNoteStorage"
import {
  getLastInstance,
  resetInstance,
} from "@tests/helpers/aiReplyEventSourceTracker"

class MockIntersectionObserver {
  readonly root: Element | null = null
  readonly rootMargin: string = "0px"
  readonly thresholds: ReadonlyArray<number> = [0]

  observe = vi.fn()
  unobserve = vi.fn()
  disconnect = vi.fn()
  takeRecords = vi.fn().mockReturnValue([])
}

beforeAll(() => {
  // Mock IntersectionObserver
  global.IntersectionObserver = MockIntersectionObserver

  // Mock window.performance
  Object.defineProperty(window, "performance", {
    value: {
      now: () => Date.now(),
    },
    configurable: true,
  })
})

// Mock AiReplyEventSource to track instances
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

const mountComponent = (conversation, storageAccessor) => {
  return helper
    .component(AiResponse)
    .withProps({ conversation, aiReplyTrigger: 0, storageAccessor })
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

describe("ConversationInner", () => {
  let wrapper
  let note
  let noteRealm
  let conversation
  let storageAccessor

  beforeEach(() => {
    storageAccessor = createNoteStorage()
    resetInstance()
    mockShowNote()

    const testData = setupTestData()
    note = testData.note
    noteRealm = testData.noteRealm
    conversation = testData.conversation

    wrapper = mountComponent(conversation, storageAccessor)
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
    const testCompletion = "**bold completion**"
    const renderedCompletion = "bold completion"
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
          completion: testCompletion,
        })
      )
    })

    it("formats completion suggestion correctly based on existing content", async () => {
      // Test empty note details
      noteRealm.note.details = ""
      storageAccessor.refreshNoteRealm(noteRealm)
      await submitMessageAndSimulateRunResponse(
        wrapper,
        createToolCallChunk("NoteDetailsCompletion", {
          completion: testCompletion,
          deleteFromEnd: 0,
        })
      )
      expect(wrapper.find(".completion-text").text()).toBe(renderedCompletion)

      // Test with existing note details
      noteRealm.note.details = "Existing content"
      storageAccessor.refreshNoteRealm(noteRealm)
      await submitMessageAndSimulateRunResponse(
        wrapper,
        createToolCallChunk("NoteDetailsCompletion", {
          completion: testCompletion,
          deleteFromEnd: 0,
        })
      )
      expect(wrapper.find(".completion-text").text()).toBe(
        `...${renderedCompletion}`
      )
    })

    it("formats completion suggestion with strikethrough for deleted content", async () => {
      noteRealm.note.details = "Hello world"
      storageAccessor.refreshNoteRealm(noteRealm)
      await submitMessageAndSimulateRunResponse(
        wrapper,
        createToolCallChunk("NoteDetailsCompletion", {
          completion: " friends!",
          deleteFromEnd: 5,
        })
      )

      // The markdown ~~world~~ should render as strikethrough text
      expect(wrapper.find(".completion-text").text()).toBe("world friends!")
      expect(wrapper.find(".completion-text").html()).toContain(
        "<del>world</del>"
      )
    })

    it("handles strikethrough when deleteFromEnd is larger than existing content", async () => {
      noteRealm.note.details = "Short\ntext"
      storageAccessor.refreshNoteRealm(noteRealm)
      await submitMessageAndSimulateRunResponse(
        wrapper,
        createToolCallChunk("NoteDetailsCompletion", {
          completion: "New content",
          deleteFromEnd: 20,
        })
      )

      // The entire existing text should be struck through
      expect(wrapper.find(".completion-text").text()).toBe(
        "Short↵textNew content"
      )
    })

    it("accepts the completion suggestion and updates the note", async () => {
      // Accept the suggestion
      await wrapper.find('button[class*="btn-primary"]').trigger("click")
      await flushPromises()

      expect(updateNoteDetailsSpy).toHaveBeenCalledWith({
        path: { note: note.id },
        body: { details: testCompletion },
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

    it("handles completion with character deletion", async () => {
      noteRealm.note.details = "Hello world"
      storageAccessor.refreshNoteRealm(noteRealm)
      await submitMessageAndSimulateRunResponse(
        wrapper,
        createToolCallChunk("NoteDetailsCompletion", {
          completion: " friends!",
          deleteFromEnd: 6,
        })
      )

      // Check the formatted suggestion shows with ellipsis
      expect(wrapper.find(".completion-text").text()).toBe("·world friends!")

      expect(wrapper.find(".completion-text").html()).toContain(
        "<del>·world</del>"
      )
      // Accept the suggestion
      await wrapper.find('button[class*="btn-primary"]').trigger("click")
      await flushPromises()

      // Should delete "world" and add "friends!"
      expect(updateNoteDetailsSpy).toHaveBeenCalledWith({
        path: { note: note.id },
        body: { details: "Hello friends!" },
      })
    })

    it("handles over-deletion by removing all content", async () => {
      noteRealm.note.details = "Hello world"
      storageAccessor.refreshNoteRealm(noteRealm)
      await submitMessageAndSimulateRunResponse(
        wrapper,
        createToolCallChunk("NoteDetailsCompletion", {
          completion: "Completely new text",
          deleteFromEnd: 20, // More than "Hello world" length
        })
      )

      // Accept the suggestion
      await wrapper.find('button[class*="btn-primary"]').trigger("click")
      await flushPromises()

      // Should delete everything and add new text
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

        const wrapper = mountComponent(conversation, storageAccessor)

        // Simulate completion suggestion
        await submitMessageAndSimulateRunResponse(
          wrapper,
          createToolCallChunk("NoteDetailsCompletion", {
            completion: "test completion",
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
