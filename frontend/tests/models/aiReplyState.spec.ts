import { describe, it, expect, vi } from "vitest"
import { createAiReplyStates } from "@/models/aiReplyState"
import type { RunStep, DeltaOfRunStep, JsonNode } from "@/generated/backend"

describe("aiReplyState", () => {
  const mockContext = {
    set: vi.fn(),
    append: vi.fn(),
    reset: vi.fn(),
    appendNoteDetails: vi.fn(),
  }

  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe("thread.run.step.created", () => {
    it("resets content when receiving a tool_calls step", async () => {
      const states = createAiReplyStates(mockContext)
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
      const states = createAiReplyStates(mockContext)
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
      const states = createAiReplyStates(mockContext)
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
      const states = createAiReplyStates(mockContext)
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
})
