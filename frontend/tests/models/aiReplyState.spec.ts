import { describe, it, expect, vi } from "vitest"
import { createAiReplyStates } from "@/models/aiReplyState"
import type {
  RunStep,
  DeltaOfRunStep,
  JsonNode,
  RestAiControllerService,
  Run,
} from "generated/backend"
import { DummyForGeneratingTypes } from "generated/backend"

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

  describe("thread.run.step.created", () => {
    it("resets content when receiving a tool_calls step", async () => {
      const states = createAiReplyStates(mockContext, mockAiController)
      const runStep: RunStep = {
        type: "tool_calls",
        step_details: {
          type: "tool_calls",
          tool_calls: [],
        },
      }

      await states["thread.run.step.created"]?.handleEvent(
        JSON.stringify(runStep)
      )

      expect(mockContext.set).toHaveBeenCalledWith("")
    })

    it("does not reset content for non-tool_calls steps", async () => {
      const states = createAiReplyStates(mockContext, mockAiController)
      const runStep: RunStep = {
        type: "message_creation",
        step_details: {
          type: "message_creation",
          message_creation: {},
        },
      }

      await states["thread.run.step.created"]?.handleEvent(
        JSON.stringify(runStep)
      )

      expect(mockContext.set).not.toHaveBeenCalled()
    })
  })

  describe("thread.run.step.delta", () => {
    it("appends tool call arguments from delta", async () => {
      const states = createAiReplyStates(mockContext, mockAiController)
      const stepDelta: DeltaOfRunStep = {
        delta: {
          step_details: {
            tool_calls: [
              {
                function: {
                  arguments: '{"partial": "argument"}' as unknown as JsonNode,
                },
              },
            ],
          },
        },
      }

      await states["thread.run.step.delta"]?.handleEvent(
        JSON.stringify(stepDelta)
      )

      expect(mockContext.append).toHaveBeenCalledWith('{"partial": "argument"}')
    })

    it("handles empty or malformed delta gracefully", async () => {
      const states = createAiReplyStates(mockContext, mockAiController)
      const emptyDelta: DeltaOfRunStep = {
        delta: {
          step_details: {
            tool_calls: [],
          },
        },
      }

      await states["thread.run.step.delta"]?.handleEvent(
        JSON.stringify(emptyDelta)
      )

      expect(mockContext.append).toHaveBeenCalledWith(undefined)
    })
  })

  describe("thread.run.requires_action", () => {
    const mockRun: Run = {
      id: "run-123",
      thread_id: "thread-123",
      required_action: {
        submit_tool_outputs: {
          tool_calls: [],
        },
      },
    }

    beforeEach(() => {
      mockContext.handleSuggestion.mockResolvedValue({ status: "accepted" })
    })

    it("handles single tool call successfully", async () => {
      const run = {
        ...mockRun,
        required_action: {
          submit_tool_outputs: {
            tool_calls: [
              {
                id: "call-1",
                function: {
                  name: DummyForGeneratingTypes.aiToolName
                    .COMPLETE_NOTE_DETAILS,
                  arguments: JSON.stringify({ completion: "test content" }),
                },
              },
            ],
          },
        },
      }

      const states = createAiReplyStates(mockContext, mockAiController)
      await states["thread.run.requires_action"]?.handleEvent(
        JSON.stringify(run)
      )

      expect(mockContext.handleSuggestion).toHaveBeenCalledWith({
        suggestionType: "completion",
        content: { completion: "test content" },
        threadId: "thread-123",
        runId: "run-123",
        toolCallId: "call-1",
      })
      expect(mockAiController.submitToolCallsResult).toHaveBeenCalledWith(
        "thread-123",
        "run-123",
        { "call-1": { status: "accepted" } }
      )
    })

    it("handles multiple tool calls successfully", async () => {
      const run = {
        ...mockRun,
        required_action: {
          submit_tool_outputs: {
            tool_calls: [
              {
                id: "call-1",
                function: {
                  name: DummyForGeneratingTypes.aiToolName
                    .COMPLETE_NOTE_DETAILS,
                  arguments: JSON.stringify({ completion: "test content" }),
                },
              },
              {
                id: "call-2",
                function: {
                  name: DummyForGeneratingTypes.aiToolName.SUGGEST_NOTE_TITLE,
                  arguments: JSON.stringify({ newTitle: "test title" }),
                },
              },
            ],
          },
        },
      }

      const states = createAiReplyStates(mockContext, mockAiController)
      await states["thread.run.requires_action"]?.handleEvent(
        JSON.stringify(run)
      )

      expect(mockContext.handleSuggestion).toHaveBeenCalledWith({
        suggestionType: "completion",
        content: { completion: "test content" },
        threadId: "thread-123",
        runId: "run-123",
        toolCallId: "call-1",
      })
      expect(mockContext.handleSuggestion).toHaveBeenCalledWith({
        suggestionType: "title",
        content: "test title",
        threadId: "thread-123",
        runId: "run-123",
        toolCallId: "call-2",
      })
      expect(mockAiController.submitToolCallsResult).toHaveBeenCalledWith(
        "thread-123",
        "run-123",
        {
          "call-1": { status: "accepted" },
          "call-2": { status: "accepted" },
        }
      )
    })

    it("cancels run when any tool call is rejected", async () => {
      const run = {
        ...mockRun,
        required_action: {
          submit_tool_outputs: {
            tool_calls: [
              {
                id: "call-1",
                function: {
                  name: DummyForGeneratingTypes.aiToolName
                    .COMPLETE_NOTE_DETAILS,
                  arguments: JSON.stringify({ completion: "test content" }),
                },
              },
            ],
          },
        },
      }

      mockContext.handleSuggestion.mockRejectedValue(
        new Error("Tool call was rejected")
      )

      const states = createAiReplyStates(mockContext, mockAiController)
      await states["thread.run.requires_action"]?.handleEvent(
        JSON.stringify(run)
      )

      expect(mockAiController.cancelRun).toHaveBeenCalledWith(
        "thread-123",
        "run-123"
      )
      expect(mockAiController.submitToolCallsResult).not.toHaveBeenCalled()
    })

    it("handles unknown tool call through handleSuggestion", async () => {
      const unknownToolName = "unknown_tool"
      const unknownToolArgs = { test: "data" }
      const run = {
        ...mockRun,
        required_action: {
          submit_tool_outputs: {
            tool_calls: [
              {
                id: "call-1",
                function: {
                  name: unknownToolName,
                  arguments: JSON.stringify(unknownToolArgs),
                },
              },
            ],
          },
        },
      }

      mockContext.handleSuggestion.mockResolvedValue({ status: "skipped" })

      const states = createAiReplyStates(mockContext, mockAiController)
      await states["thread.run.requires_action"]?.handleEvent(
        JSON.stringify(run)
      )

      expect(mockContext.handleSuggestion).toHaveBeenCalledWith({
        suggestionType: "unknown",
        content: {
          rawJson: JSON.stringify(unknownToolArgs),
          functionName: unknownToolName,
        },
        threadId: "thread-123",
        runId: "run-123",
        toolCallId: "call-1",
      })
      expect(mockAiController.submitToolCallsResult).toHaveBeenCalledWith(
        "thread-123",
        "run-123",
        { "call-1": { status: "skipped" } }
      )
    })
  })
})
