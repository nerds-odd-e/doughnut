import {
  NoteController,
  ConversationMessageController,
} from "@generated/donut-backend-api/sdk.gen"
import { useNoteStore } from "@/store/noteStore"
import { mockSdkService, wrapSdkError, wrapSdkResponse } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import { beforeEach, expect, it, vi } from "vitest"
import {
  body,
  mountConversation,
  realm,
  setupCorrections,
} from "./conversationCorrectionTestSupport"

vi.mock("@/managedApi/AiReplyEventSource", async () => {
  const { aiReplyEventSourceTrackingMock } = await import(
    "./aiReplyEventSourceTrackingMock"
  )
  return aiReplyEventSourceTrackingMock()
})
beforeEach(setupCorrections)

const cached = () =>
  makeMe.aNoteRealm.id(realm.id).content("Older cached wording").please()
const fresh = () =>
  makeMe.aNoteRealm.id(realm.id).content("Fresh accepted wording").please()

it("shows initial loading without cached content, then presents the fresh review", async () => {
  let resolveRead: (
    result: ReturnType<typeof wrapSdkResponse<typeof realm>>
  ) => void
  mockSdkService(NoteController, "showNote", realm).mockImplementation(
    () =>
      new Promise((resolve) => {
        resolveRead = resolve
      })
  )
  const wrapper = mountConversation()
  await flushPromises()
  const review = wrapper.find('[data-testid="conversation-note-correction"]')
  expect(review.find("[data-app-busy]").exists()).toBe(true)
  resolveRead!(wrapSdkResponse(fresh()))
  await flushPromises()
  expect(review.text()).toContain("Fresh accepted wording")
  expect(review.find("textarea").element).toBeInstanceOf(HTMLTextAreaElement)
  wrapper.unmount()
})

it("waits for fresh current content before enabling initial acceptance with the proposal base", async () => {
  let resolveRead: (
    result: ReturnType<typeof wrapSdkResponse<typeof realm>>
  ) => void
  mockSdkService(NoteController, "showNote", realm).mockImplementation(
    () =>
      new Promise((resolve) => {
        resolveRead = resolve
      })
  )
  const accept = mockSdkService(
    ConversationMessageController,
    "acceptCorrection",
    body
  )
  const wrapper = mountConversation()
  useNoteStore().refreshNoteRealm(cached())
  await flushPromises()
  const review = wrapper.find('[data-testid="conversation-note-correction"]')
  const button = () =>
    review.findAll("button").find((item) => item.text() === "Accept")!
  expect(review.text()).toContain("Older cached wording")
  expect((button().element as HTMLButtonElement).disabled).toBe(true)
  await review.find("textarea").setValue("My retained wording")
  resolveRead!(wrapSdkResponse(fresh()))
  await flushPromises()
  expect(review.text()).toContain("Fresh accepted wording")
  expect((button().element as HTMLButtonElement).disabled).toBe(false)
  await button().trigger("click")
  await flushPromises()
  expect(accept).toHaveBeenCalledWith({
    path: { correctionId: body.id },
    body: {
      reviewedValue: "My retained wording",
      baseRevision: body.baseRevision,
    },
  })
  wrapper.unmount()
})

it("keeps acceptance unavailable after an initial read failure and retains the draft through retry", async () => {
  const read = mockSdkService(NoteController, "showNote", realm)
  read.mockResolvedValueOnce(wrapSdkError({ message: "Read failed" }))
  const wrapper = mountConversation()
  useNoteStore().refreshNoteRealm(cached())
  await flushPromises()
  const review = wrapper.find('[data-testid="conversation-note-correction"]')
  const button = (label: string) =>
    review.findAll("button").find((item) => item.text() === label)!
  expect(review.text()).toContain(
    "Could not load the current note. Your review is kept"
  )
  expect((button("Accept").element as HTMLButtonElement).disabled).toBe(true)
  await review.find("textarea").setValue("My retained wording")
  read.mockResolvedValue(wrapSdkResponse(fresh()))
  await button("Retry loading current content").trigger("click")
  await flushPromises()
  expect(review.text()).toContain("Fresh accepted wording")
  expect((review.find("textarea").element as HTMLTextAreaElement).value).toBe(
    "My retained wording"
  )
  expect((button("Accept").element as HTMLButtonElement).disabled).toBe(false)
  wrapper.unmount()
})

it("retains its fresh observed content when an older overlapping read finishes afterward", async () => {
  let resolveOlder: (
    result: ReturnType<typeof wrapSdkResponse<typeof realm>>
  ) => void
  const read = mockSdkService(NoteController, "showNote", fresh())
  read.mockImplementationOnce(
    () =>
      new Promise((resolve) => {
        resolveOlder = resolve
      })
  )
  const olderRead = useNoteStore().loadNoteRealm(realm.id)
  const accept = mockSdkService(
    ConversationMessageController,
    "acceptCorrection",
    body
  )
  const wrapper = mountConversation()
  await flushPromises()
  const review = wrapper.find('[data-testid="conversation-note-correction"]')
  expect(review.text()).toContain("Fresh accepted wording")
  resolveOlder!(wrapSdkResponse(cached()))
  await olderRead
  await flushPromises()
  expect(useNoteStore().refOfNoteRealm(realm.id).value?.note.content).toBe(
    "Older cached wording"
  )
  expect(review.text()).toContain("Fresh accepted wording")
  await review.find("textarea").setValue("My reviewed wording")
  await review
    .findAll("button")
    .find((button) => button.text() === "Accept")!
    .trigger("click")
  await flushPromises()
  expect(accept).toHaveBeenCalledWith({
    path: { correctionId: body.id },
    body: {
      reviewedValue: "My reviewed wording",
      baseRevision: body.baseRevision,
    },
  })
  wrapper.unmount()
})
