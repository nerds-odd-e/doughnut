import {
  AiAudioController,
  TextContentController,
} from "@generated/donut-backend-api/sdk.gen"
import type { NoteRealm } from "@generated/donut-backend-api"
import NoteTextContent from "@/components/notes/core/NoteTextContent.vue"
import NoteMoreOptionsActions from "@/components/notes/widgets/NoteMoreOptionsActions.vue"
import NoteVoiceInputButton from "@/components/notes/widgets/NoteVoiceInputButton.vue"
import { useNoteStore } from "@/store/noteStore"
import makeMe from "donut-test-fixtures/makeMe"
import helper, { mockSdkServiceWithImplementation } from "@tests/helpers"
import { advanceNoteContentSaveDebounce } from "@tests/helpers/noteContentDebounceTestSupport"
import {
  audioChunk,
  audioTextResponse,
  processAudio,
  startRecording,
  voiceInputVm,
  type NoteVoiceInputButtonWrapper,
} from "@tests/notes/noteVoiceInputButtonTestSupport"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach } from "vitest"
import { defineComponent, h, ref } from "vue"

export const dictatedPassage = "The orchard path leads down to the river."

/**
 * Mounts the editable body editor beside the toolbar actions that hold Voice
 * input, with the dictation request held until the test has typed. Call inside
 * a describe block, after useNoteVoiceInputTestLifecycle().
 */
export function useBodyEditorWithHeldDictation(passage = dictatedPassage) {
  const noteStore = useNoteStore()
  const editorNoteId = ref(0)
  let wrapper: NoteVoiceInputButtonWrapper | undefined
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
        return audioTextResponse(passage)
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

  function mountEditorAndVoiceInput(body: string, asMarkdown: boolean) {
    const note = makeMe.aNoteRealm.content(body).please()
    editorNoteId.value = note.id
    const builder = helper
      .component(
        defineComponent({
          setup() {
            return () => {
              const pageNote = noteStore.refOfNoteRealm(editorNoteId.value)
                .value!.note
              return h("div", [
                h(NoteTextContent, {
                  note: pageNote,
                  readonly: false,
                  asMarkdown,
                  wikiLinks: [],
                }),
                h(NoteMoreOptionsActions, {
                  note: pageNote,
                  layout: "toolbar",
                }),
              ])
            }
          },
        })
      )
      .withCleanStorage()
      .withRouter()
    noteStore.refreshNoteRealm(note)
    const mounted = builder.mount({ attachTo: document.body })
    wrapper = mounted
    return { wrapper: mounted, note }
  }

  /** The author moves the page to another note. */
  async function showInEditor(other: NoteRealm) {
    noteStore.refreshNoteRealm(other)
    editorNoteId.value = other.id
    await flushPromises()
  }

  /** The author records, then leaves; the stop on leaving converts the remainder, held until they have left. */
  async function whileRecordingIsLeft(leave: () => Promise<void>) {
    const button = wrapper!.findComponent(
      NoteVoiceInputButton
    ) as NoteVoiceInputButtonWrapper
    await startRecording(button)
    const { audioRecorder, processAudio: convert } = voiceInputVm(button)
    audioRecorder.stopRecording.mockImplementation(async () => {
      await convert(audioChunk())
    })
    await leave()
    releaseAudio()
    await flushPromises()
    await advanceNoteContentSaveDebounce()
  }

  async function dictate(whilePending?: () => Promise<void>) {
    await flushPromises()
    const processing = processAudio(
      wrapper!.findComponent(
        NoteVoiceInputButton
      ) as NoteVoiceInputButtonWrapper
    )
    await flushPromises()
    await whilePending?.()
    releaseAudio()
    await processing
    await flushPromises()
  }

  async function whileAudioIsPending(type: () => Promise<void>) {
    await dictate(type)
    await advanceNoteContentSaveDebounce()
  }

  function savedContents(noteId?: number) {
    return updateContentMock.mock.calls
      .filter(
        ([options]) => noteId === undefined || options.path.note === noteId
      )
      .map(([options]) => options.body?.content)
  }

  function lastSavedContent(noteId?: number) {
    return savedContents(noteId).at(-1)
  }

  return {
    mountEditorAndVoiceInput,
    showInEditor,
    whileRecordingIsLeft,
    dictate,
    whileAudioIsPending,
    savedContents,
    lastSavedContent,
  }
}
