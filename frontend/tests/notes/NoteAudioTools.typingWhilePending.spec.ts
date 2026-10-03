import {
  AiAudioController,
  TextContentController,
} from "@generated/donut-backend-api/sdk.gen"
import NoteTextContent from "@/components/notes/core/NoteTextContent.vue"
import NoteAudioTools from "@/components/notes/widgets/NoteAudioTools.vue"
import { useNoteStore } from "@/store/noteStore"
import makeMe from "donut-test-fixtures/makeMe"
import helper, { mockSdkServiceWithImplementation } from "@tests/helpers"
import { advanceNoteContentSaveDebounce } from "@tests/helpers/noteContentDebounceTestSupport"
import { holdNoteContentSave } from "@tests/notes/noteTextContentTestSupport"
import {
  dictatedTextResponse,
  processAudio,
  useNoteAudioToolsTestLifecycle,
  type NoteAudioToolsWrapper,
} from "@tests/notes/noteAudioToolsTestSupport"
import {
  richQuillInstance,
  setTextareaValue,
  textareaEl,
} from "@tests/notes/noteEditableContentTestSupport"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { defineComponent, h, type ComponentPublicInstance } from "vue"

vi.mock("@/models/audio/recorderWorklet", async () => {
  const { recorderWorkletMockExports } = await import(
    "@tests/notes/noteAudioToolsMocks"
  )
  return recorderWorkletMockExports()
})

vi.mock("@/models/audio/audioRecorder", async () => {
  const { audioRecorderMockExports } = await import(
    "@tests/notes/noteAudioToolsMocks"
  )
  return audioRecorderMockExports()
})

vi.mock("@/models/wakeLocker", async () => {
  const { wakeLockerMockExports } = await import(
    "@tests/notes/noteAudioToolsMocks"
  )
  return wakeLockerMockExports()
})

useNoteAudioToolsTestLifecycle()

const passage = " The orchard path leads down to the river."

describe("NoteAudioTools while the author types in the open body editor", () => {
  const noteStore = useNoteStore()
  let wrapper: VueWrapper<ComponentPublicInstance>
  let updateContentMock: ReturnType<typeof mockSdkServiceWithImplementation>
  let releaseAudio: () => void

  beforeEach(() => {
    const audioHeld = new Promise<void>((resolve) => {
      releaseAudio = resolve
    })
    mockSdkServiceWithImplementation(
      AiAudioController,
      "audioToText",
      async () => {
        await audioHeld
        return dictatedTextResponse(passage)
      }
    )
    updateContentMock = mockSdkServiceWithImplementation(
      TextContentController,
      "updateNoteContent",
      async (options) =>
        makeMe.aNoteRealm
          .id(options.path.note)
          .content(options.body?.content ?? "")
          .please()
    )
  })

  afterEach(() => {
    wrapper?.unmount()
  })

  function mountEditorAndAudioTools(body: string, asMarkdown: boolean) {
    const realm = makeMe.aNoteRealm.content(body).please()
    const builder = helper
      .component(
        defineComponent({
          setup() {
            const current = noteStore.refOfNoteRealm(realm.id)
            return () =>
              h("div", [
                h(NoteTextContent, {
                  note: current.value!.note,
                  readonly: false,
                  asMarkdown,
                  wikiLinks: [],
                }),
                h(NoteAudioTools, { note: current.value!.note }),
              ])
          },
        })
      )
      .withCleanStorage()
      .withRouter()
    noteStore.refreshNoteRealm(realm)
    wrapper = builder.mount({ attachTo: document.body })
    return realm
  }

  async function whileAudioIsPending(type: () => Promise<void>) {
    await flushPromises()
    const processing = processAudio(
      wrapper.findComponent(NoteAudioTools) as NoteAudioToolsWrapper
    )
    await flushPromises()
    await type()
    releaseAudio()
    await processing
    await flushPromises()
    await advanceNoteContentSaveDebounce()
  }

  function lastSavedContent() {
    return updateContentMock.mock.lastCall?.[0].body?.content
  }

  const redBicycleBody = [
    "Original paragraph one: The museum opens at nine each morning. Our tickets are booked for Tuesday.",
    "Original paragraph two: The blue notebook contains the garden measurements. Keep the oak tree map beside it.",
    "The lighthouse keeper painted the front door bright yellow. Tomorrow we will bring fresh oranges to the beach.",
  ].join("\n\n")
  const typed = " MANUAL EDIT: Keep this red bicycle sentence."

  it("keeps typing at the end of the rich editor and saves the passage after it", async () => {
    mountEditorAndAudioTools(redBicycleBody, false)
    const quill = () => richQuillInstance(wrapper)

    await whileAudioIsPending(async () => {
      quill().insertText(quill().getLength() - 1, typed, "user")
      await flushPromises()
    })

    expect(quill().getText()).toContain(`${typed}${passage}\n`)
    expect(lastSavedContent()).toBe(`${redBicycleBody}${typed}${passage}`)
  })

  it("keeps typing at the end of the Markdown editor and saves the passage after it", async () => {
    mountEditorAndAudioTools(redBicycleBody, true)

    await whileAudioIsPending(async () => {
      await setTextareaValue(wrapper, `${redBicycleBody}${typed}`)
    })

    expect(textareaEl(wrapper).value).toBe(
      `${redBicycleBody}${typed}${passage}`
    )
    expect(lastSavedContent()).toBe(`${redBicycleBody}${typed}${passage}`)
  })

  it("keeps a correction in the middle and puts the passage at the end", async () => {
    const body =
      "The book I bought yesterday is a gift from my sister. She enjoys gardens."
    mountEditorAndAudioTools(body, false)
    const quill = () => richQuillInstance(wrapper)

    await whileAudioIsPending(async () => {
      const at = quill().getText().indexOf("from my sister")
      quill().deleteText(at, "from".length, "user")
      quill().insertText(at, "for", "user")
      await flushPromises()
    })

    expect(lastSavedContent()).toBe(
      `${body.replace("from my sister", "for my sister")}${passage}`
    )
  })

  it("saves the passage as soon as it joins the open editor's draft", async () => {
    mountEditorAndAudioTools(redBicycleBody, true)
    await flushPromises()
    const processing = processAudio(
      wrapper.findComponent(NoteAudioTools) as NoteAudioToolsWrapper
    )
    releaseAudio()
    await processing
    await flushPromises()

    expect(lastSavedContent()).toBe(`${redBicycleBody}${passage}`)
  })

  it("keeps typing that was already saved and saves the passage after it once", async () => {
    mountEditorAndAudioTools(redBicycleBody, true)

    await whileAudioIsPending(async () => {
      await setTextareaValue(wrapper, `${redBicycleBody}${typed}`)
      await advanceNoteContentSaveDebounce()
      expect(lastSavedContent()).toBe(`${redBicycleBody}${typed}`)
    })

    expect(textareaEl(wrapper).value).toBe(
      `${redBicycleBody}${typed}${passage}`
    )
    expect(lastSavedContent()).toBe(`${redBicycleBody}${typed}${passage}`)
  })

  it("keeps typing whose save is still in flight and saves the passage after it once", async () => {
    const realm = mountEditorAndAudioTools(redBicycleBody, true)
    const releaseSave = holdNoteContentSave((saved) =>
      makeMe.aNoteRealm.id(realm.id).content(saved).please()
    )

    await whileAudioIsPending(async () => {
      await setTextareaValue(wrapper, `${redBicycleBody}${typed}`)
      await advanceNoteContentSaveDebounce()
    })
    releaseSave()
    await flushPromises()

    expect(lastSavedContent()).toBe(`${redBicycleBody}${typed}${passage}`)
  })
})
