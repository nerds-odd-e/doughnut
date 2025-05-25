import AiResponse from "@/components/conversations/AiResponse.vue"
import { expect, vi } from "vitest"
import helper from "@tests/helpers"
import makeMe from "@tests/fixtures/makeMe"
import {
  DummyForGeneratingTypes,
  type Message,
  type MessageDelta,
  type TitleReplacement,
} from "generated/backend"
import { flushPromises } from "@vue/test-utils"
import createNoteStorage from "@/store/createNoteStorage"

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

export const simulateAiResponse = (content = "## I'm ChatGPT") => {
  const newMessage: Message = {
    role: "assistant",
    thread_id: "test-thread-id",
    content: [],
  }
  const messageDelta: MessageDelta = {
    delta: {
      content: [
        {
          text: {
            value: content,
          },
        },
      ],
    },
  }

  helper.managedApi.eventSource.eventSourceRequest.onMessage(
    "thread.message.created",
    JSON.stringify(newMessage)
  )
  helper.managedApi.eventSource.eventSourceRequest.onMessage(
    "thread.message.delta",
    JSON.stringify(messageDelta)
  )
}

const createRunResponse = (functionName: string, args: object) => ({
  id: "run-123",
  thread_id: "thread-123",
  required_action: {
    submit_tool_outputs: {
      tool_calls: [
        {
          id: "call-456",
          function: {
            name: functionName,
            arguments: JSON.stringify(args),
          },
        },
      ],
    },
  },
})

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

const submitMessageAndSimulateRunResponse = async (wrapper, runResponse) => {
  await submitMessage(wrapper)
  helper.managedApi.eventSource.eventSourceRequest.onMessage(
    "thread.run.requires_action",
    JSON.stringify(runResponse)
  )
  await flushPromises()
}

describe("ConversationInner", () => {
  let wrapper
  let note
  let noteRealm
  let conversation
  let storageAccessor

  beforeEach(() => {
    storageAccessor = createNoteStorage(helper.managedApi)
    helper.managedApi.eventSource.restConversationMessageController.getAiReply =
      vi.fn()

    const testData = setupTestData()
    note = testData.note
    noteRealm = testData.noteRealm
    conversation = testData.conversation

    wrapper = mountComponent(conversation, storageAccessor)
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

        // After message created
        helper.managedApi.eventSource.eventSourceRequest.onMessage(
          "thread.message.created",
          JSON.stringify({
            role: "assistant",
            thread_id: "test-thread-id",
            content: [],
          })
        )
        await wrapper.vm.$nextTick()
        expect(statusText()).toBe("Generating response...")

        // During writing
        helper.managedApi.eventSource.eventSourceRequest.onMessage(
          "thread.message.delta",
          JSON.stringify({
            delta: {
              content: [{ text: { value: "Test" } }],
            },
          })
        )
        await wrapper.vm.$nextTick()
        expect(statusText()).toBe("Writing response...")

        // After completion
        helper.managedApi.eventSource.eventSourceRequest.onMessage("done", "")
        await wrapper.vm.$nextTick()
        expect(wrapper.find(".status-bar").exists()).toBe(false)
      })

      it("hides status bar and shows error message on failure", async () => {
        const onError =
          helper.managedApi.eventSource.eventSourceRequest!.onError
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
    const threadId = "thread-123"
    const runId = "run-123"
    const toolCallId = "call-456"

    beforeEach(async () => {
      helper.managedApi.restAiController.submitToolCallsResult = vi.fn()
      helper.managedApi.restAiController.cancelRun = vi.fn()
      helper.managedApi.restTextContentController.updateNoteDetails = vi.fn()

      await submitMessageAndSimulateRunResponse(
        wrapper,
        createRunResponse(
          DummyForGeneratingTypes.aiToolName.COMPLETE_NOTE_DETAILS,
          {
            completion: testCompletion,
          }
        )
      )
    })

    it("formats completion suggestion correctly based on existing content", async () => {
      // Test empty note details
      noteRealm.note.details = ""
      storageAccessor.refreshNoteRealm(noteRealm)
      await submitMessageAndSimulateRunResponse(
        wrapper,
        createRunResponse(
          DummyForGeneratingTypes.aiToolName.COMPLETE_NOTE_DETAILS,
          { completion: testCompletion, deleteFromEnd: 0 }
        )
      )
      expect(wrapper.find(".completion-text").text()).toBe(renderedCompletion)

      // Test with existing note details
      noteRealm.note.details = "Existing content"
      storageAccessor.refreshNoteRealm(noteRealm)
      await submitMessageAndSimulateRunResponse(
        wrapper,
        createRunResponse(
          DummyForGeneratingTypes.aiToolName.COMPLETE_NOTE_DETAILS,
          { completion: testCompletion, deleteFromEnd: 0 }
        )
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
        createRunResponse(
          DummyForGeneratingTypes.aiToolName.COMPLETE_NOTE_DETAILS,
          { completion: " friends!", deleteFromEnd: 5 }
        )
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
        createRunResponse(
          DummyForGeneratingTypes.aiToolName.COMPLETE_NOTE_DETAILS,
          { completion: "New content", deleteFromEnd: 20 }
        )
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

      expect(
        helper.managedApi.restTextContentController.updateNoteDetails
      ).toHaveBeenCalledWith(note.id, { details: testCompletion })

      expect(
        helper.managedApi.restAiController.submitToolCallsResult
      ).toHaveBeenCalledWith(threadId, runId, {
        [toolCallId]: { status: "accepted" },
      })

      expect(wrapper.find(".completion-text").exists()).toBe(false)
    })

    it("cancel the completion suggestion without updating the note", async () => {
      // Cancel the suggestion
      await wrapper.find('button[class*="btn-secondary"]').trigger("click")
      await flushPromises()

      expect(
        helper.managedApi.restTextContentController.updateNoteDetails
      ).not.toHaveBeenCalled()

      expect(helper.managedApi.restAiController.cancelRun).toHaveBeenCalledWith(
        threadId,
        runId
      )

      expect(wrapper.find(".completion-text").exists()).toBe(false)
    })

    it("skips the completion suggestion without updating the note", async () => {
      // Skip the suggestion
      await wrapper
        .find('button[class*="btn-outline-secondary"]')
        .trigger("click")
      await flushPromises()

      expect(
        helper.managedApi.restTextContentController.updateNoteDetails
      ).not.toHaveBeenCalled()

      expect(
        helper.managedApi.restAiController.submitToolCallsResult
      ).toHaveBeenCalledWith(threadId, runId, {
        [toolCallId]: { status: "skipped" },
      })

      expect(wrapper.find(".completion-text").exists()).toBe(false)
    })

    it("handles completion with character deletion", async () => {
      noteRealm.note.details = "Hello world"
      storageAccessor.refreshNoteRealm(noteRealm)
      await submitMessageAndSimulateRunResponse(
        wrapper,
        createRunResponse(
          DummyForGeneratingTypes.aiToolName.COMPLETE_NOTE_DETAILS,
          {
            completion: " friends!",
            deleteFromEnd: 6,
          }
        )
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
      expect(
        helper.managedApi.restTextContentController.updateNoteDetails
      ).toHaveBeenCalledWith(note.id, { details: "Hello friends!" })
    })

    it("handles over-deletion by removing all content", async () => {
      noteRealm.note.details = "Hello world"
      storageAccessor.refreshNoteRealm(noteRealm)
      await submitMessageAndSimulateRunResponse(
        wrapper,
        createRunResponse(
          DummyForGeneratingTypes.aiToolName.COMPLETE_NOTE_DETAILS,
          {
            completion: "Completely new text",
            deleteFromEnd: 20, // More than "Hello world" length
          }
        )
      )

      // Accept the suggestion
      await wrapper.find('button[class*="btn-primary"]').trigger("click")
      await flushPromises()

      // Should delete everything and add new text
      expect(
        helper.managedApi.restTextContentController.updateNoteDetails
      ).toHaveBeenCalledWith(note.id, { details: "Completely new text" })
    })

    describe("Note Access", () => {
      beforeEach(async () => {
        helper.managedApi.restAiController.submitToolCallsResult = vi.fn()
        helper.managedApi.restTextContentController.updateNoteDetails = vi.fn()
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
          createRunResponse(
            DummyForGeneratingTypes.aiToolName.COMPLETE_NOTE_DETAILS,
            {
              completion: "test completion",
            }
          )
        )

        // Try to accept the completion
        await wrapper.find('button[class*="btn-primary"]').trigger("click")
        await flushPromises()

        expect(
          helper.managedApi.restTextContentController.updateNoteDetails
        ).toHaveBeenCalled()
      })
    })
  })

  describe("Title Title Generation", () => {
    const testTitle = "Generated Title"
    const threadId = "thread-123"
    const runId = "run-123"
    const toolCallId = "call-456"

    beforeEach(async () => {
      helper.managedApi.restAiController.submitToolCallsResult = vi.fn()
      helper.managedApi.restAiController.cancelRun = vi.fn()
      helper.managedApi.restTextContentController.updateNoteTitle = vi.fn()

      await submitMessageAndSimulateRunResponse(
        wrapper,
        createRunResponse(
          DummyForGeneratingTypes.aiToolName.SUGGEST_NOTE_TITLE,
          <TitleReplacement>{
            newTitle: testTitle,
          }
        )
      )
    })

    it("accepts the title suggestion and updates the note", async () => {
      await wrapper.find('button[class*="btn-primary"]').trigger("click")
      await flushPromises()

      expect(
        helper.managedApi.restTextContentController.updateNoteTitle
      ).toHaveBeenCalledWith(note.id, { newTitle: testTitle })

      expect(
        helper.managedApi.restAiController.submitToolCallsResult
      ).toHaveBeenCalledWith(threadId, runId, {
        [toolCallId]: { status: "accepted" },
      })

      expect(wrapper.find(".title-suggestion").exists()).toBe(false)
    })

    it("rejects the title suggestion without updating the note", async () => {
      await wrapper.find('button[class*="btn-secondary"]').trigger("click")
      await flushPromises()

      expect(
        helper.managedApi.restTextContentController.updateNoteTitle
      ).not.toHaveBeenCalled()

      expect(helper.managedApi.restAiController.cancelRun).toHaveBeenCalledWith(
        threadId,
        runId
      )

      expect(wrapper.find(".title-suggestion").exists()).toBe(false)
    })

    it("skips the title suggestion without updating the note", async () => {
      await wrapper
        .find('button[class*="btn-outline-secondary"]')
        .trigger("click")
      await flushPromises()

      expect(
        helper.managedApi.restTextContentController.updateNoteTitle
      ).not.toHaveBeenCalled()

      expect(
        helper.managedApi.restAiController.submitToolCallsResult
      ).toHaveBeenCalledWith(threadId, runId, {
        [toolCallId]: { status: "skipped" },
      })

      expect(wrapper.find(".title-suggestion").exists()).toBe(false)
    })
  })

  describe("Unknown Tool Call Handling", () => {
    const testJson = { unknown: "data" }
    const threadId = "thread-123"
    const runId = "run-123"
    const toolCallId = "call-456"

    beforeEach(async () => {
      helper.managedApi.restAiController.submitToolCallsResult = vi.fn()
      helper.managedApi.restAiController.cancelRun = vi.fn()

      const run = createRunResponse("unknown_tool", testJson)

      await submitMessageAndSimulateRunResponse(wrapper, run)
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

      expect(
        helper.managedApi.restAiController.submitToolCallsResult
      ).toHaveBeenCalledWith(threadId, runId, {
        [toolCallId]: { status: "skipped" },
      })

      expect(wrapper.find(".unknown-request").exists()).toBe(false)
    })

    it("cancels the unknown request", async () => {
      await wrapper.find('button[class*="btn-secondary"]').trigger("click")
      await flushPromises()

      expect(helper.managedApi.restAiController.cancelRun).toHaveBeenCalledWith(
        threadId,
        runId
      )

      expect(wrapper.find(".unknown-request").exists()).toBe(false)
    })
  })
})
