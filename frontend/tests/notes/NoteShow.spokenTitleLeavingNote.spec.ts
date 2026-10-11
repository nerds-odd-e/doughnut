import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import type { NoteRealm } from "@generated/donut-backend-api"
import NoteShow from "@/components/notes/NoteShow.vue"
import { createAudioRecorder } from "@/models/audio/audioRecorder"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import type { ComponentPublicInstance } from "vue"
import makeMe from "donut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import { noteTitleText } from "@tests/notes/noteNewFormTestSupport"
import {
  expectIdleSpeakTitleButton,
  speakAndStop,
  speakTheTitle,
  titleEditable,
  useSpokenTitleTestLifecycle,
} from "@tests/notes/spokenTitleTestSupport"
import { titleEditorEl } from "@tests/notes/noteTextContentTestSupport"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

vi.mock("@/models/audio/recorderWorklet", async () => {
  const { recorderWorkletMockExports } = await import(
    "@tests/notes/noteVoiceInputButtonMocks"
  )
  return recorderWorkletMockExports()
})

vi.mock("@/models/audio/audioRecorder", async () => {
  const { audioRecorderInvokingCallbackMockExports } = await import(
    "@tests/notes/noteVoiceInputButtonMocks"
  )
  return audioRecorderInvokingCallbackMockExports()
})

useSpokenTitleTestLifecycle("harvest")

describe("Speaking a title when the author moves to another note", () => {
  let wrapper: VueWrapper<ComponentPublicInstance>
  const origin = makeMe.aNoteRealm.title("Orchard notes").please()
  const destination = makeMe.aNoteRealm.title("Pear tree").please()

  async function show(realm: NoteRealm) {
    mockSdkService(NoteController, "showNote", realm)
    await wrapper.setProps({ noteId: realm.id })
    await flushPromises()
  }

  beforeEach(async () => {
    mockSdkService(NoteController, "showNote", destination)
    wrapper = helper
      .component(NoteShow)
      .withRouter()
      .withCleanStorage()
      .withCurrentUser(makeMe.aUser.please())
      .withProps({ noteId: destination.id })
      .mount({ attachTo: document.body })
    await flushPromises()
    await show(origin)
  })

  afterEach(() => {
    wrapper.unmount()
  })

  it("stops listening and leaves the title of the note they arrive at editable, with its own text and without focus", async () => {
    await speakTheTitle(wrapper)
    expect(titleEditable(wrapper)).toBe("false")
    const recorder = vi.mocked(createAudioRecorder).mock.results.at(-1)!.value

    await show(destination)

    expect(recorder.stopRecording).toHaveBeenCalledTimes(1)
    expect(titleEditable(wrapper)).toBe("true")
    expect(noteTitleText(wrapper)).toBe("Pear tree")
    expect(document.activeElement).not.toBe(titleEditorEl(wrapper))
    expectIdleSpeakTitleButton(wrapper)
  })

  it("speaks into the title of the note they arrive at afterwards", async () => {
    await speakTheTitle(wrapper)
    await show(destination)

    await speakAndStop(wrapper)

    expect(noteTitleText(wrapper)).toBe("Pear tree harvest")
  })
})
