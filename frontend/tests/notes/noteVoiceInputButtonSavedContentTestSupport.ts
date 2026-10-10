import {
  AiAudioController,
  TextContentController,
} from "@generated/donut-backend-api/sdk.gen"
import { useNoteStore } from "@/store/noteStore"
import makeMe from "donut-test-fixtures/makeMe"
import {
  mockSdkService,
  mockSdkServiceWithImplementation,
} from "@tests/helpers"
import {
  audioTextResponse,
  mountNoteVoiceInputButton,
  type NoteVoiceInputButtonWrapper,
} from "@tests/notes/noteVoiceInputButtonTestSupport"
import { afterEach, beforeEach } from "vitest"

/** Mounts dictation against a saved body, with content PATCH responses updating the store. */
export function useSavedBodyDictation(body = "") {
  const originalRealm = makeMe.aNoteRealm.content(body).please()
  const note = originalRealm.note
  const noteStore = useNoteStore()
  let wrapper: NoteVoiceInputButtonWrapper
  let audioToTextMock: ReturnType<typeof mockSdkService>
  let updateContentMock: ReturnType<typeof mockSdkServiceWithImplementation>

  beforeEach(() => {
    audioToTextMock = mockSdkService(
      AiAudioController,
      "audioToText",
      audioTextResponse("text")
    )
    wrapper = mountNoteVoiceInputButton(note)
    noteStore.refreshNoteRealm(originalRealm)
    updateContentMock = mockSdkServiceWithImplementation(
      TextContentController,
      "updateNoteContent",
      (options) =>
        makeMe.aNoteRealm
          .id(options.path.note)
          .content(options.body.content ?? "")
          .please()
    )
  })

  afterEach(() => {
    wrapper?.unmount()
  })

  return {
    note,
    noteStore,
    get wrapper() {
      return wrapper
    },
    get audioToTextMock() {
      return audioToTextMock
    },
    get updateContentMock() {
      return updateContentMock
    },
  }
}
