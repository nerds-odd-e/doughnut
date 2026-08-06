import { TextContentController } from "@generated/doughnut-backend-api/sdk.gen"
import makeMe from "doughnut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import {
  createToolCallChunk,
  mountAiResponse,
  submitMessageAndSimulateToolCall,
  useAiResponseMount,
} from "./aiResponseTestSupport"

vi.mock("@/managedApi/AiReplyEventSource", async () => {
  const { aiReplyEventSourceTrackingMock } = await import(
    "./aiReplyEventSourceTrackingMock"
  )
  return aiReplyEventSourceTrackingMock()
})

describe("AiResponse note content completion", () => {
  const ctx = useAiResponseMount()
  const suggestedCompletion = "**bold completion**"
  let updateNoteContentSpy: ReturnType<typeof mockSdkService>

  beforeEach(async () => {
    ctx.beforeEachMount("")
    updateNoteContentSpy = mockSdkService(
      TextContentController,
      "updateNoteContent",
      makeMe.aNoteRealm.please()
    )

    await submitMessageAndSimulateToolCall(
      ctx.wrapper,
      createToolCallChunk("NoteContentCompletion", {
        content: suggestedCompletion,
      })
    )
  })

  afterEach(() => {
    ctx.afterEachReset()
  })

  it("formats completion suggestion for empty note content", () => {
    expect(ctx.wrapper.find(".completion-text").html()).toContain(
      "<strong>bold completion</strong>"
    )
  })

  it("formats completion suggestion with existing note content", async () => {
    ctx.refreshNoteContent("Existing content")
    await submitMessageAndSimulateToolCall(
      ctx.wrapper,
      createToolCallChunk("NoteContentCompletion", {
        content: "Existing content\n**bold completion**",
      })
    )
    const html = ctx.wrapper.find(".completion-text").html()
    expect(html).toContain("Existing content")
    expect(html).toContain("<strong>bold completion</strong>")
  })

  it("formats completion suggestion replacing overlapping content", async () => {
    ctx.refreshNoteContent("Hello world")
    await submitMessageAndSimulateToolCall(
      ctx.wrapper,
      createToolCallChunk("NoteContentCompletion", {
        content: "Hello  friends!",
      })
    )

    expect(ctx.wrapper.find(".completion-text").html()).toContain(
      "Hello friends!"
    )
  })

  it("formats completion when replacing all content", async () => {
    ctx.refreshNoteContent("Short\ntext")
    await submitMessageAndSimulateToolCall(
      ctx.wrapper,
      createToolCallChunk("NoteContentCompletion", {
        content: "New content",
      })
    )

    expect(ctx.wrapper.find(".completion-text").html()).toContain("New content")
  })

  it("accepts the completion suggestion and updates the note", async () => {
    await ctx.wrapper.find('button[class*="btn-primary"]').trigger("click")
    await flushPromises()

    expect(updateNoteContentSpy).toHaveBeenCalledWith({
      path: { note: ctx.note.id },
      body: { content: "**bold completion**" },
    })
    expect(ctx.wrapper.find(".completion-text").exists()).toBe(false)
  })

  it("cancels the completion suggestion without updating the note", async () => {
    vi.clearAllMocks()

    await ctx.wrapper.find('button[class*="btn-secondary"]').trigger("click")
    await flushPromises()

    expect(updateNoteContentSpy).not.toHaveBeenCalled()
    expect(ctx.wrapper.find(".completion-text").exists()).toBe(false)
  })

  it("skips the completion suggestion without updating the note", async () => {
    vi.clearAllMocks()

    await ctx.wrapper
      .find("button.daisy-btn-outline.daisy-btn-secondary")
      .trigger("click")
    await flushPromises()

    expect(updateNoteContentSpy).not.toHaveBeenCalled()
    expect(ctx.wrapper.find(".completion-text").exists()).toBe(false)
  })

  it("accepts completion with replacement of overlapping content", async () => {
    ctx.refreshNoteContent("Hello world")
    await submitMessageAndSimulateToolCall(
      ctx.wrapper,
      createToolCallChunk("NoteContentCompletion", {
        content: "Hello  friends!",
      })
    )

    await ctx.wrapper.find('button[class*="btn-primary"]').trigger("click")
    await flushPromises()

    expect(updateNoteContentSpy).toHaveBeenCalledWith({
      path: { note: ctx.note.id },
      body: { content: "Hello  friends!" },
    })
  })

  it("accepts completion that replaces all content", async () => {
    ctx.refreshNoteContent("Hello world")
    await submitMessageAndSimulateToolCall(
      ctx.wrapper,
      createToolCallChunk("NoteContentCompletion", {
        content: "Completely new text",
      })
    )

    await ctx.wrapper.find('button[class*="btn-primary"]').trigger("click")
    await flushPromises()

    expect(updateNoteContentSpy).toHaveBeenCalledWith({
      path: { note: ctx.note.id },
      body: { content: "Completely new text" },
    })
  })

  it("handles completion when note is only on answeredQuestion subject", async () => {
    const answeredQuestion = makeMe.anAnsweredQuestion
      .withNote(ctx.note)
      .please()
    const conversation = makeMe.aConversation
      .forAnsweredQuestion(answeredQuestion)
      .please()

    const wrapper = mountAiResponse(conversation)
    await submitMessageAndSimulateToolCall(
      wrapper,
      createToolCallChunk("NoteContentCompletion", {
        content: "test completion",
      })
    )

    await wrapper.find('button[class*="btn-primary"]').trigger("click")
    await flushPromises()

    expect(updateNoteContentSpy).toHaveBeenCalled()
  })
})
