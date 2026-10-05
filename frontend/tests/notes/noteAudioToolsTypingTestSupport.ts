import {
  AiAudioController,
  TextContentController,
} from "@generated/donut-backend-api/sdk.gen"
import type { NoteRealm } from "@generated/donut-backend-api"
import NoteTextContent from "@/components/notes/core/NoteTextContent.vue"
import NoteAudioTools from "@/components/notes/widgets/NoteAudioTools.vue"
import { useNoteStore } from "@/store/noteStore"
import makeMe from "donut-test-fixtures/makeMe"
import helper, { mockSdkServiceWithImplementation } from "@tests/helpers"
import { advanceNoteContentSaveDebounce } from "@tests/helpers/noteContentDebounceTestSupport"
import {
  audioTextResponse,
  processAudio,
  type NoteAudioToolsWrapper,
} from "@tests/notes/noteAudioToolsTestSupport"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach } from "vitest"
import { defineComponent, h, ref } from "vue"

export const dictatedPassage = "The orchard path leads down to the river."

/**
 * Mounts the editable body editor beside NoteAudioTools, with the dictation
 * request held until the test has typed. Call inside a describe block, after
 * useNoteAudioToolsTestLifecycle().
 */
export function useBodyEditorWithHeldDictation(passage = dictatedPassage) {
  const noteStore = useNoteStore()
  const editorNoteId = ref(0)
  let wrapper: NoteAudioToolsWrapper | undefined
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

  function mountEditorAndAudioTools(body: string, asMarkdown: boolean) {
    const note = makeMe.aNoteRealm.content(body).please()
    editorNoteId.value = note.id
    const builder = helper
      .component(
        defineComponent({
          setup() {
            const audioNote = noteStore.refOfNoteRealm(note.id)
            return () =>
              h("div", [
                h(NoteTextContent, {
                  note: noteStore.refOfNoteRealm(editorNoteId.value).value!
                    .note,
                  readonly: false,
                  asMarkdown,
                  wikiLinks: [],
                }),
                h(NoteAudioTools, { note: audioNote.value!.note }),
              ])
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

  /** The author moves the body editor to another note; dictation stays bound to the first. */
  async function showInEditor(other: NoteRealm) {
    noteStore.refreshNoteRealm(other)
    editorNoteId.value = other.id
    await flushPromises()
  }

  async function dictate(whilePending?: () => Promise<void>) {
    await flushPromises()
    const processing = processAudio(
      wrapper!.findComponent(NoteAudioTools) as NoteAudioToolsWrapper
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
    mountEditorAndAudioTools,
    showInEditor,
    dictate,
    whileAudioIsPending,
    savedContents,
    lastSavedContent,
  }
}
