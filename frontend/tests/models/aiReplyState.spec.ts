import { describe, it, expect, vi } from "vitest"
import { createAiReplyStates } from "@/models/aiReplyState"
import type { RestAiControllerService } from "@generated/backend/services/RestAiControllerService"
import { DummyForGeneratingTypes } from "@generated/backend"

describe("aiReplyState", () => {
  const mockContext = {
    set: vi.fn(),
    append: vi.fn(),
    reset: vi.fn(),
    handleSuggestion: vi.fn(),
  }

  const mockAiController = {
    submitToolCallsResult: vi.fn().mockResolvedValue({}),
    cancelRun: vi.fn(),
  } as unknown as RestAiControllerService

  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe("chat.completion.chunk", () => {
    it("appends content from message", async () => {
      const states = createAiReplyStates(mockContext, mockAiController)
      const chunk = {
        choices: [
          {
            index: 0,
            message: {
              role: "assistant",
              content: "Hello world",
            },
            finish_reason: null,
          },
        ],
      }

      await states["chat.completion.chunk"]?.handleEvent(JSON.stringify(chunk))

      expect(mockContext.append).toHaveBeenCalledWith("Hello world")
    })

    it("handles tool call successfully", async () => {
      mockContext.handleSuggestion.mockResolvedValue({ status: "accepted" })

      const chunk = {
        choices: [
          {
            index: 0,
            message: {
              role: "assistant",
              content: null,
              tool_calls: [
                {
                  id: "call-1",
                  type: "function",
                  function: {
                    name: DummyForGeneratingTypes.aiToolName
                      .COMPLETE_NOTE_DETAILS,
                    arguments: JSON.stringify({ completion: "test content" }),
                  },
                },
              ],
            },
            finish_reason: "tool_calls",
          },
        ],
      }

      const states = createAiReplyStates(mockContext, mockAiController)
      await states["chat.completion.chunk"]?.handleEvent(JSON.stringify(chunk))

      expect(mockContext.handleSuggestion).toHaveBeenCalledWith({
        suggestionType: "completion",
        content: { completion: "test content" },
        threadId: "synthetic",
        runId: "synthetic",
        toolCallId: "call-1",
      })
      expect(mockAiController.submitToolCallsResult).toHaveBeenCalledWith(
        "synthetic",
        "synthetic",
        { "call-1": { status: "accepted" } }
      )
    })

    it("handles title suggestion tool call", async () => {
      mockContext.handleSuggestion.mockResolvedValue({ status: "accepted" })

      const chunk = {
        choices: [
          {
            index: 0,
            message: {
              role: "assistant",
              content: null,
              tool_calls: [
                {
                  id: "call-1",
                  type: "function",
                  function: {
                    name: DummyForGeneratingTypes.aiToolName.SUGGEST_NOTE_TITLE,
                    arguments: JSON.stringify({ newTitle: "New Title" }),
                  },
                },
              ],
            },
            finish_reason: "tool_calls",
          },
        ],
      }

      const states = createAiReplyStates(mockContext, mockAiController)
      await states["chat.completion.chunk"]?.handleEvent(JSON.stringify(chunk))

      expect(mockContext.handleSuggestion).toHaveBeenCalledWith({
        suggestionType: "title",
        content: "New Title",
        threadId: "synthetic",
        runId: "synthetic",
        toolCallId: "call-1",
      })
    })

    it("cancels when tool call is rejected", async () => {
      mockContext.handleSuggestion.mockRejectedValue(
        new Error("Tool call was rejected")
      )

      const chunk = {
        choices: [
          {
            index: 0,
            message: {
              tool_calls: [
                {
                  id: "call-1",
                  function: {
                    name: DummyForGeneratingTypes.aiToolName
                      .COMPLETE_NOTE_DETAILS,
                    arguments: JSON.stringify({ completion: "test" }),
                  },
                },
              ],
            },
            finish_reason: "tool_calls",
          },
        ],
      }

      const states = createAiReplyStates(mockContext, mockAiController)
      await states["chat.completion.chunk"]?.handleEvent(JSON.stringify(chunk))

      expect(mockAiController.cancelRun).toHaveBeenCalledWith(
        "synthetic",
        "synthetic"
      )
      expect(mockAiController.submitToolCallsResult).not.toHaveBeenCalled()
    })

    it("handles unknown tool call", async () => {
      mockContext.handleSuggestion.mockResolvedValue({ status: "skipped" })

      const chunk = {
        choices: [
          {
            index: 0,
            message: {
              tool_calls: [
                {
                  id: "call-1",
                  function: {
                    name: "unknown_tool",
                    arguments: JSON.stringify({ test: "data" }),
                  },
                },
              ],
            },
            finish_reason: "tool_calls",
          },
        ],
      }

      const states = createAiReplyStates(mockContext, mockAiController)
      await states["chat.completion.chunk"]?.handleEvent(JSON.stringify(chunk))

      expect(mockContext.handleSuggestion).toHaveBeenCalledWith({
        suggestionType: "unknown",
        content: {
          rawJson: JSON.stringify({ test: "data" }),
          functionName: "unknown_tool",
        },
        threadId: "synthetic",
        runId: "synthetic",
        toolCallId: "call-1",
      })
    })
  })

  describe("done", () => {
    it("resets context", async () => {
      const states = createAiReplyStates(mockContext, mockAiController)
      await states.done?.handleEvent("")

      expect(mockContext.reset).toHaveBeenCalled()
    })
  })
})
