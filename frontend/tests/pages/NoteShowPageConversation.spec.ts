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
import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, expect, it, vi } from "vitest"

describe("note show page conversation", () => {
  let router: ReturnType<typeof createNoteShowPageRouter>
  let noteId: number

  beforeEach(() => {
    router = createNoteShowPageRouter()
    noteId = setupNoteShowPageConversationMocks().id
  })

  it("maximizes and restores note content when maximize is toggled", async () => {
    await renderNoteShowPageWithConversation(router, noteId)

    await vi.waitFor(() => {
      expect(toggleMaximizeButtonEl()).not.toBeNull()
    })

    toggleMaximizeButtonEl()!.click()
    await flushPromises()
    expect(noteContentWrapperEl()).toBeNull()

    toggleMaximizeButtonEl()!.click()
    await flushPromises()
    expect(noteContentWrapperEl()).not.toBeNull()
  })

  it("restores note content and clears conversation query on close", async () => {
    await renderNoteShowPageWithConversation(router, noteId)

    await vi.waitFor(() => {
      expect(toggleMaximizeButtonEl()).not.toBeNull()
    })

    toggleMaximizeButtonEl()!.click()
    await flushPromises()

    closeConversationButtonEl()!.click()
    await flushPromises()

    expect(router.currentRoute.value.query.conversation).toBeUndefined()
    expect(noteContentWrapperEl()).not.toBeNull()
    expect(conversationContainerEl()).toBeNull()
  })

  it("opens conversation when URL has conversation=true", async () => {
    await renderNoteShowPageWithConversation(router, noteId)

    await vi.waitFor(() => {
      expect(conversationWrapperEl()).not.toBeNull()
    })
  })
})
