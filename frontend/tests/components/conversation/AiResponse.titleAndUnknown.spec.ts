import { TextContentController } from "@generated/doughnut-backend-api/sdk.gen"
import type { TitleReplacement } from "@generated/doughnut-backend-api"
import makeMe from "doughnut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
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

describe("AiResponse title suggestion", () => {
  const ctx = useAiResponseMount()
  const testTitle = "Generated Title"
  let updateNoteTitleSpy: ReturnType<typeof mockSdkService>

  beforeEach(async () => {
    ctx.beforeEachMount()
    updateNoteTitleSpy = mockSdkService(
      TextContentController,
      "updateNoteTitle",
      makeMe.aNoteRealm.please()
    )

    await submitMessageAndSimulateToolCall(
      ctx.wrapper,
      createToolCallChunk("TitleReplacement", <TitleReplacement>{
        newTitle: testTitle,
      })
    )
  })

  afterEach(() => {
    ctx.afterEachReset()
  })

  it("accepts the title suggestion and updates the note", async () => {
    await ctx.wrapper.find('button[class*="btn-primary"]').trigger("click")
    await flushPromises()

    expect(updateNoteTitleSpy).toHaveBeenCalledWith({
      path: { note: ctx.note.id },
      body: { newTitle: testTitle },
    })
    expect(ctx.wrapper.find(".title-suggestion").exists()).toBe(false)
  })

  it("rejects the title suggestion without updating the note", async () => {
    vi.clearAllMocks()

    await ctx.wrapper.find('button[class*="btn-secondary"]').trigger("click")
    await flushPromises()

    expect(updateNoteTitleSpy).not.toHaveBeenCalled()
    expect(ctx.wrapper.find(".title-suggestion").exists()).toBe(false)
  })

  it("skips the title suggestion without updating the note", async () => {
    vi.clearAllMocks()

    await ctx.wrapper
      .find("button.daisy-btn-outline.daisy-btn-secondary")
      .trigger("click")
    await flushPromises()

    expect(updateNoteTitleSpy).not.toHaveBeenCalled()
    expect(ctx.wrapper.find(".title-suggestion").exists()).toBe(false)
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
    expect(ctx.wrapper.find(".ai-chat").text()).toContain("unknown_tool")
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
