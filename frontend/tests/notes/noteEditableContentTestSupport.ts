import {
  MemoryTrackerController,
  NoteController,
  TextContentController,
} from "@generated/doughnut-backend-api/sdk.gen"
import NoteEditableContent from "@/components/notes/core/NoteEditableContent.vue"
import type { UpdateNoteContentData } from "@generated/doughnut-backend-api"
import usePopups from "@/components/commons/Popups/usePopups"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import type { ComponentPublicInstance } from "vue"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import { vi } from "vitest"

export const markdownTextareaDefaults = {
  readonly: false,
  asMarkdown: true,
  wikiTitles: [] as string[],
}

export const trackedPropertyNoteId = 42
export const trackedPropertyMarkdown = `---
topic: training
---

Workshop body.`

export function mountNoteEditableContent(
  props: {
    noteId: number
    noteContent?: string
    readonly?: boolean
    asMarkdown?: boolean
    wikiTitles?: string[]
  },
  options?: { attachTo?: HTMLElement }
) {
  const chain = helper
    .component(NoteEditableContent)
    .withCleanStorage()
    .withRouter()
    .withProps({ ...markdownTextareaDefaults, ...props })
  return options?.attachTo
    ? chain.mount({ attachTo: options.attachTo })
    : chain.mount()
}

export async function mountMarkdownTextarea(props: {
  noteId: number
  noteContent: string
}) {
  const wrapper = mountNoteEditableContent(props)
  await flushPromises()
  return wrapper
}

export function textareaEl(wrapper: VueWrapper<ComponentPublicInstance>) {
  return wrapper.find("textarea").element as HTMLTextAreaElement
}

export async function setTextareaValue(
  wrapper: VueWrapper<ComponentPublicInstance>,
  value: string
) {
  const el = textareaEl(wrapper)
  el.value = value
  el.dispatchEvent(new Event("input"))
  await flushPromises()
  return el
}

export async function blurTextarea(
  wrapper: VueWrapper<ComponentPublicInstance>
) {
  await wrapper.find("textarea").trigger("blur")
  await flushPromises()
}

export function createClipboardEvent(html: string): ClipboardEvent {
  const event = new ClipboardEvent("paste", {
    bubbles: true,
    cancelable: true,
    clipboardData: new DataTransfer(),
  })
  event.clipboardData?.setData("text/html", html)
  return event
}

export function setupUpdateNoteContentMock() {
  return mockSdkService(
    TextContentController,
    "updateNoteContent",
    makeMe.aNoteRealm.please()
  )
}

// biome-ignore lint/suspicious/noExplicitAny: Mock type for testing
export function setupPopupsMock(mockPopupsOptions: any) {
  vi.mocked(usePopups).mockReturnValue({
    popups: {
      options: mockPopupsOptions,
      alert: vi.fn(),
      confirm: vi.fn(),
      done: vi.fn(),
      register: vi.fn(),
      peek: vi.fn(),
    },
  })
}

export function setupMemoryTrackerSdkMocks() {
  const getNoteInfoSpy = mockSdkService(NoteController, "getNoteInfo", {
    memoryTrackers: [],
  })
  const softDeleteSpy = mockSdkService(
    MemoryTrackerController,
    "softDelete",
    undefined
  )
  const updatePropertyKeySpy = mockSdkService(
    MemoryTrackerController,
    "updatePropertyKey",
    undefined
  )
  return { getNoteInfoSpy, softDeleteSpy, updatePropertyKeySpy }
}

export function mockNoteInfoWithPropertyTracker(
  getNoteInfoSpy: ReturnType<typeof mockSdkService>,
  key: string,
  id: number
) {
  const tracker = makeMe.aMemoryTracker.please()
  tracker.id = id
  tracker.propertyKey = key
  getNoteInfoSpy.mockResolvedValue(
    wrapSdkResponse(makeMe.aNoteRecallInfo.memoryTrackers([tracker]).please())
  )
  return tracker
}

export function mockDelayedFirstSave(
  updateNoteContentSpy: ReturnType<typeof mockSdkService>,
  noteId: number
) {
  let resolveFirstSave: (() => void) | undefined
  const firstSavePromise = new Promise<void>((resolve) => {
    resolveFirstSave = resolve
  })

  updateNoteContentSpy.mockImplementation((async (
    options: UpdateNoteContentData
  ) => {
    if (options.body?.content === "First edit") {
      await firstSavePromise
    }
    return wrapSdkResponse({
      id: noteId,
      note: {
        id: noteId,
        content: options.body?.content,
        noteTopology: { id: noteId, title: "Test Note" },
      },
    })
    // biome-ignore lint/suspicious/noExplicitAny: Vitest mock typing requires any for implementation functions
  }) as any)

  return () => {
    resolveFirstSave!()
  }
}
