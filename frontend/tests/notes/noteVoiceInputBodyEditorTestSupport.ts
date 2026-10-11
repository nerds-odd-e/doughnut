import {
  AiAudioController,
  TextContentController,
} from "@generated/donut-backend-api/sdk.gen"
import type { NoteRealm } from "@generated/donut-backend-api"
import NoteTextContent from "@/components/notes/core/NoteTextContent.vue"
import NoteMoreOptionsActions from "@/components/notes/widgets/NoteMoreOptionsActions.vue"
import NoteVoiceInputButton from "@/components/notes/widgets/NoteVoiceInputButton.vue"
import { noteVoiceInputTitles } from "@/components/notes/widgets/noteMoreOptionsTitles"
import { useNoteStore } from "@/store/noteStore"
import makeMe from "donut-test-fixtures/makeMe"
import helper, {
  mockSdkServiceWithImplementation,
  wrapSdkError,
} from "@tests/helpers"
import {
  audioChunk,
  audioTextResponse,
  midSpeechChunk,
  processAudio,
  startRecording,
  stopRecording,
  voiceInputVm,
  type NoteVoiceInputButtonWrapper,
} from "@tests/notes/noteVoiceInputButtonTestSupport"
import {
  richQuillInstance,
  textareaEl,
} from "@tests/notes/noteEditableContentTestSupport"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach } from "vitest"
import { defineComponent, h, ref } from "vue"

/**
 * Mounts the editable body editor beside the toolbar actions that hold Voice
 * input. The transcription service answers with the texts given to `hears`,
 * in order. Call inside a describe block, after
 * useNoteVoiceInputTestLifecycle().
 */
export function useBodyEditorWithVoiceInput() {
  const noteStore = useNoteStore()
  const editorNoteId = ref(0)
  const editsAsMarkdown = ref(false)
  let wrapper: NoteVoiceInputButtonWrapper | undefined
  let audioToTextMock: ReturnType<typeof mockSdkServiceWithImplementation>
  let updateContentMock: ReturnType<typeof mockSdkServiceWithImplementation>
  let heard: (string | string[])[]
  let audioHeld: Promise<void> | undefined

  beforeEach(() => {
    heard = []
    audioHeld = undefined
    audioToTextMock = mockSdkServiceWithImplementation(
      AiAudioController,
      "audioToText",
      async () => {
        await audioHeld
        return audioTextResponse(heard.shift()!)
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

  const button = () =>
    wrapper!.findComponent(NoteVoiceInputButton) as NoteVoiceInputButtonWrapper

  /** Mounts the note with Voice input idle; Stop and retry convert what remains. */
  function mountEditorAndVoiceInput(body: string, asMarkdown: boolean) {
    const note = makeMe.aNoteRealm.content(body).please()
    editorNoteId.value = note.id
    editsAsMarkdown.value = asMarkdown
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
                  asMarkdown: editsAsMarkdown.value,
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
    const { audioRecorder, processAudio: convert } = voiceInputVm(button())
    audioRecorder.stopRecording.mockImplementation(async () => {
      await convert(audioChunk()).catch(() => undefined)
    })
    return { wrapper: mounted, note }
  }

  /** Mounts the rich editor with the author's caret or selection placed. */
  async function mountRichEditorWithSelection(
    body: string,
    index: number,
    length = 0
  ) {
    const { wrapper } = mountEditorAndVoiceInput(body, false)
    await flushPromises()
    richQuillInstance(wrapper).setSelection(index, length, "user")
    return wrapper
  }

  /** Mounts the Markdown editor with the author's caret or selection placed. */
  async function mountMarkdownEditorWithSelection(
    body: string,
    from: number,
    to = from
  ) {
    const { wrapper } = mountEditorAndVoiceInput(body, true)
    await flushPromises()
    const el = textareaEl(wrapper)
    el.focus()
    el.setSelectionRange(from, to)
    await wrapper.find("textarea").trigger("mouseup")
    return el
  }

  /** What the following conversions return, one each: a text, or its segments. */
  function hears(...texts: (string | string[])[]) {
    heard.push(...texts)
  }

  /** Conversions wait until the returned function is called. */
  function holdAudio() {
    let release!: () => void
    audioHeld = new Promise<void>((resolve) => {
      release = resolve
    })
    return async () => {
      release()
      await flushPromises()
    }
  }

  /** The conversion at Stop fails and leaves a recording to retry. */
  function conversionFailsKeepingTheRecording() {
    audioToTextMock.mockResolvedValueOnce(wrapSdkError("API Error"))
    voiceInputVm(button()).audioRecorder.hasUnconvertedAudio.mockReturnValue(
      true
    )
  }

  function microphoneCannotStart() {
    voiceInputVm(button()).audioRecorder.startRecording.mockRejectedValueOnce(
      new Error("Permission denied")
    )
  }

  const start = () => startRecording(button())

  /** A passage is converted while the author is still speaking. */
  async function passageArrives(text: string) {
    hears(text)
    await processAudio(button(), midSpeechChunk())
    await flushPromises()
  }

  const stop = () => stopRecording(button())

  async function retry() {
    await button()
      .find(`button[aria-label="${noteVoiceInputTitles.retry}"]`)
      .trigger("click")
    await flushPromises()
  }

  async function editAsMarkdown(asMarkdown: boolean) {
    editsAsMarkdown.value = asMarkdown
    await flushPromises()
  }

  /** The author moves the page to another note. */
  async function showInEditor(other: NoteRealm) {
    noteStore.refreshNoteRealm(other)
    editorNoteId.value = other.id
    await flushPromises()
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
    mountRichEditorWithSelection,
    mountMarkdownEditorWithSelection,
    hears,
    holdAudio,
    conversionFailsKeepingTheRecording,
    microphoneCannotStart,
    start,
    passageArrives,
    stop,
    retry,
    editAsMarkdown,
    showInEditor,
    savedContents,
    lastSavedContent,
  }
}
