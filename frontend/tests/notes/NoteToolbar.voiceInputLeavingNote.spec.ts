import {
  AiAudioController,
  TextContentController,
} from "@generated/donut-backend-api/sdk.gen"
import type { NoteRealm } from "@generated/donut-backend-api"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService, wrapSdkError } from "@tests/helpers"
import {
  audioTextResponse,
  expectIdleVoiceInputInToolbar,
  mountNoteToolbarRecording,
  moveNoteToolbarTo,
  useNoteVoiceInputTestLifecycle,
} from "@tests/notes/noteVoiceInputButtonTestSupport"
import {
  showToastsOnPage,
  toastMessagesOnPage,
  toastShown,
} from "@tests/helpers/toastTestSupport"
import { noteVoiceInputTitles } from "@/components/notes/widgets/noteMoreOptionsTitles"
import { noteToolbarAction } from "@tests/notes/noteToolbarTestHelpers"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

vi.mock("@/models/audio/recorderWorklet", async () => {
  const { recorderWorkletMockExports } = await import(
    "@tests/notes/noteVoiceInputButtonMocks"
  )
  return recorderWorkletMockExports()
})

vi.mock("@/models/audio/audioRecorder", async () => {
  const { audioRecorderMockExports } = await import(
    "@tests/notes/noteVoiceInputButtonMocks"
  )
  return audioRecorderMockExports()
})

vi.mock("@/models/wakeLocker", async () => {
  const { wakeLockerMockExports } = await import(
    "@tests/notes/noteVoiceInputButtonMocks"
  )
  return wakeLockerMockExports()
})

useNoteVoiceInputTestLifecycle()
showToastsOnPage()

describe("NoteToolbar Voice input when the author moves to another note", () => {
  let wrapper: VueWrapper
  let recorder: Awaited<
    ReturnType<typeof mountNoteToolbarRecording>
  >["recorder"]
  let audioToText: ReturnType<typeof mockSdkService>
  let saveContent: ReturnType<typeof mockSdkService>
  let origin: NoteRealm
  let destination: NoteRealm

  beforeEach(async () => {
    saveContent = mockSdkService(
      TextContentController,
      "updateNoteContent",
      makeMe.aNoteRealm.please()
    )
    audioToText = mockSdkService(
      AiAudioController,
      "audioToText",
      audioTextResponse("The ferry returns at six.")
    )
    origin = makeMe.aNoteRealm.content("Origin body.").please()
    destination = makeMe.aNoteRealm.content("Destination body.").please()
    ;({ wrapper, recorder } = await mountNoteToolbarRecording(origin))
  })

  afterEach(() => {
    wrapper?.unmount()
  })

  it("converts the remainder into the note the author left", async () => {
    await moveNoteToolbarTo(wrapper, destination)

    expect(recorder.stopRecording).toHaveBeenCalledTimes(1)
    expect(saveContent).toHaveBeenCalledExactlyOnceWith({
      path: { note: origin.note.id },
      body: { content: "Origin body. The ferry returns at six." },
    })
    expectIdleVoiceInputInToolbar(wrapper)
  })

  describe("and the speech cannot be turned into text", () => {
    beforeEach(() => {
      audioToText.mockResolvedValue(wrapSdkError("API Error"))
      recorder.hasUnconvertedAudio.mockReturnValue(true)
    })

    it("drops a kept recording, and the button is idle", async () => {
      await noteToolbarAction(wrapper, noteVoiceInputTitles.stop).trigger(
        "click"
      )
      await flushPromises()
      expect(
        noteToolbarAction(wrapper, noteVoiceInputTitles.retry).exists()
      ).toBe(true)

      await moveNoteToolbarTo(wrapper, destination)

      expectIdleVoiceInputInToolbar(wrapper)
      expect(audioToText).toHaveBeenCalledTimes(1)
      expect(saveContent).not.toHaveBeenCalled()
    })

    it("toasts only that the speech was not turned into text when the final conversion on leaving fails, keeping nothing", async () => {
      await moveNoteToolbarTo(wrapper, destination)

      await toastShown("error")
      expect(toastMessagesOnPage()).toEqual([
        "Could not turn your speech into text.",
      ])
      expectIdleVoiceInputInToolbar(wrapper)
      expect(saveContent).not.toHaveBeenCalled()
    })
  })
})
