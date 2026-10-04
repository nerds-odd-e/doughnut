import {
  closeConversationButtonEl,
  conversationContainerEl,
  noteContentWrapperEl,
  renderNoteShowPageWithConversation,
  setupNoteShowPageConversationMocks,
  toggleMaximizeButtonEl,
} from "@tests/pages/noteShowPageTestSupport"
import { productionRouterAt } from "@tests/helpers"
import {
  noteShowLocation,
  notePropertyLocation,
} from "@/routes/noteShowLocation"
import { flushPromises } from "@vue/test-utils"
import { nextTick } from "vue"
import type { Router } from "vue-router"
import { beforeEach, describe, expect, it } from "vitest"

describe("note show page conversation", () => {
  let router: Router
  let noteId: number

  beforeEach(async () => {
    noteId = setupNoteShowPageConversationMocks().id
    router = await productionRouterAt(noteShowLocation(noteId))
  })

  it("maximizes, restores, and closes the conversation", async () => {
    await renderNoteShowPageWithConversation(router, noteId)

    const maximize = toggleMaximizeButtonEl()
    expect(maximize).not.toBeNull()

    maximize!.click()
    await nextTick()
    expect(noteContentWrapperEl()).toBeNull()

    maximize!.click()
    await nextTick()
    expect(noteContentWrapperEl()).not.toBeNull()

    maximize!.click()
    await nextTick()

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
})
