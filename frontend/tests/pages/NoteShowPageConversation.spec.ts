import {
  closeConversationButtonEl,
  conversationContainerEl,
  conversationWrapperEl,
  createNoteShowPageRouter,
  noteContentWrapperEl,
  renderNoteShowPageWithConversation,
  setupNoteShowPageConversationMocks,
  toggleMaximizeButtonEl,
} from "@tests/pages/noteShowPageTestSupport"
import { notePropertyLocation } from "@/routes/noteShowLocation"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, expect, it, vi } from "vitest"

describe("note show page conversation", () => {
  const router = createNoteShowPageRouter()
  let noteId: number

  beforeEach(() => {
    noteId = setupNoteShowPageConversationMocks().id
  })

  it("maximizes and restores note content when maximize is toggled", async () => {
    await renderNoteShowPageWithConversation(router, noteId)

    const maximize = toggleMaximizeButtonEl()
    expect(maximize).not.toBeNull()

    maximize!.click()
    await flushPromises()
    expect(noteContentWrapperEl()).toBeNull()

    maximize!.click()
    await flushPromises()
    expect(noteContentWrapperEl()).not.toBeNull()
  })

  it("restores note content and clears conversation query on close", async () => {
    await renderNoteShowPageWithConversation(router, noteId)

    const maximize = toggleMaximizeButtonEl()
    expect(maximize).not.toBeNull()

    maximize!.click()
    await flushPromises()

    closeConversationButtonEl()!.click()
    await flushPromises()

    expect(router.currentRoute.value.query.conversation).toBeUndefined()
    expect(noteContentWrapperEl()).not.toBeNull()
    expect(conversationContainerEl()).toBeNull()
  })

  it("clears conversation query without leaving the property location", async () => {
    await renderNoteShowPageWithConversation(router, noteId, {
      ...notePropertyLocation(noteId, "topic"),
      query: { conversation: "true" },
    })

    closeConversationButtonEl()!.click()
    await flushPromises()

    expect(router.currentRoute.value).toMatchObject(
      notePropertyLocation(noteId, "topic")
    )
  })

  it("opens conversation when URL has conversation=true", async () => {
    await renderNoteShowPageWithConversation(router, noteId)

    await vi.waitFor(() => {
      expect(conversationWrapperEl()).not.toBeNull()
    })
  })
})
