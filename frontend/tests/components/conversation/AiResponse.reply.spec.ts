import { getLastInstance } from "@tests/helpers/aiReplyEventSourceTracker"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import {
  createToolCallChunk,
  simulateAiResponse,
  submitAiReply,
  submitMessageAndSimulateToolCall,
  useAiResponseMount,
} from "./aiResponseTestSupport"

vi.mock("@/managedApi/AiReplyEventSource", async () => {
  const { aiReplyEventSourceTrackingMock } = await import(
    "./aiReplyEventSourceTrackingMock"
  )
  return aiReplyEventSourceTrackingMock()
})

describe("AiResponse streaming", () => {
  const ctx = useAiResponseMount()

  beforeEach(() => {
    ctx.beforeEachMount()
  })

  afterEach(() => {
    ctx.afterEachReset()
  })

  describe("AI Reply", () => {
    beforeEach(async () => {
      await submitAiReply(ctx.wrapper)
    })

    it("processes AI response and displays content", async () => {
      simulateAiResponse()
      await flushPromises()

      expect(ctx.wrapper.find(".ai-assistant h2").text()).toEqual("I'm ChatGPT")
    })

    it("shows status messages during AI reply lifecycle", async () => {
      const statusBar = ctx.wrapper.find(".status-bar")
      const statusText = () => statusBar.find("small").text()

      expect(statusText()).toBe("Starting AI reply...")

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
      await ctx.wrapper.vm.$nextTick()
      expect(statusText()).toBe("Streaming response...")

      instance.onMessageCallback("done", "")
      await ctx.wrapper.vm.$nextTick()
      expect(ctx.wrapper.find(".status-bar").exists()).toBe(false)
    })

    it("hides status bar and shows error message on failure", async () => {
      const instance = getLastInstance()
      if (!instance) {
        throw new Error("No AiReplyEventSource instance available")
      }
      const onError = instance.onErrorCallback
      if (!onError) throw new Error("onError is not defined")

      onError(new Error("400 Bad Request"))
      await ctx.wrapper.vm.$nextTick()

      expect(ctx.wrapper.find(".status-bar").exists()).toBe(false)
      expect(ctx.wrapper.find(".last-error-message").text()).toBe("Bad Request")
    })
  })
})

describe("AiResponse unknown tool call", () => {
  const ctx = useAiResponseMount()
  const testJson = { unknown: "data" }

  beforeEach(async () => {
    ctx.beforeEachMount()
    await submitMessageAndSimulateToolCall(
      ctx.wrapper,
      createToolCallChunk("unknown_tool", testJson)
    )
  })

  afterEach(() => {
    ctx.afterEachReset()
  })

  it("displays unknown tool call with raw JSON", () => {
    const unknownRequest = ctx.wrapper.find(".unknown-request")
    expect(unknownRequest.text()).toContain(JSON.stringify(testJson))
    expect(ctx.wrapper.find(".ai-assistant").text()).toContain("unknown_tool")
  })

  it("has no accept button for unknown tool calls", () => {
    expect(ctx.wrapper.find('button[class*="btn-primary"]').exists()).toBe(
      false
    )
  })

  it("skips the unknown request", async () => {
    await ctx.wrapper
      .find("button.daisy-btn-outline.daisy-btn-secondary")
      .trigger("click")
    await flushPromises()

    expect(ctx.wrapper.find(".unknown-request").exists()).toBe(false)
  })

  it("cancels the unknown request", async () => {
    await ctx.wrapper.find('button[class*="btn-secondary"]').trigger("click")
    await flushPromises()

    expect(ctx.wrapper.find(".unknown-request").exists()).toBe(false)
  })
})
