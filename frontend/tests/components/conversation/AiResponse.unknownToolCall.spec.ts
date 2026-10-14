import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import {
  createToolCallChunk,
  submitMessageAndSimulateToolCall,
  useAiResponseMount,
} from "./aiResponseTestSupport"

vi.mock("@/managedApi/AiReplyEventSource", async () => {
  const { aiReplyEventSourceTrackingMock } = await import(
    "./aiReplyEventSourceTrackingMock"
  )
  return aiReplyEventSourceTrackingMock()
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
