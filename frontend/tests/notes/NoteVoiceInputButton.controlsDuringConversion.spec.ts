import {
  AiAudioController,
  TextContentController,
} from "@generated/donut-backend-api/sdk.gen"
import makeMe from "donut-test-fixtures/makeMe"
import {
  mockSdkService,
  mockSdkServiceWithImplementation,
  wrapSdkError,
} from "@tests/helpers"
import { useNoteStore } from "@/store/noteStore"
import {
  audioTextResponse,
  expectIdleVoiceInputButton,
  midSpeechChunk,
  mountNoteVoiceInputButton,
  processAudio,
  startRecording,
  stopRecording,
  useNoteVoiceInputTestLifecycle,
  voiceInputButton,
  voiceInputVm,
  type NoteVoiceInputButtonWrapper,
} from "@tests/notes/noteVoiceInputButtonTestSupport"
import { noToastShown, showToastsOnPage } from "@tests/helpers/toastTestSupport"
import { flushPromises } from "@vue/test-utils"
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

describe("NoteVoiceInputButton during and after a conversion", () => {
  let wrapper: NoteVoiceInputButtonWrapper
  let saveContent: ReturnType<typeof mockSdkService>
  const realm = makeMe.aNoteRealm.please()

  beforeEach(() => {
    wrapper = mountNoteVoiceInputButton(realm.note)
    useNoteStore().refreshNoteRealm(realm)
    saveContent = mockSdkService(
      TextContentController,
      "updateNoteContent",
      makeMe.aNoteRealm.please()
    )
  })

  afterEach(() => {
    wrapper?.unmount()
  })

  const recorder = () => voiceInputVm(wrapper).audioRecorder

  const convertsTo = (segmentTexts: string[]) =>
    mockSdkService(
      AiAudioController,
      "audioToText",
      audioTextResponse(segmentTexts)
    )

  /** Holds the conversion until the returned function is called. */
  const holdConversion = (passage: string) => {
    let finish!: () => void
    const held = new Promise<void>((resolve) => (finish = resolve))
    mockSdkServiceWithImplementation(
      AiAudioController,
      "audioToText",
      async () => {
        await held
        return audioTextResponse(passage)
      }
    )
    return finish
  }

  const stopConvertsTheRest = () =>
    recorder().stopRecording.mockImplementation(async () => {
      await processAudio(wrapper)
    })

  it("keeps recording when a mid-speech conversion finishes", async () => {
    convertsTo(["test"])
    await startRecording(wrapper)

    await processAudio(wrapper, midSpeechChunk())
    await flushPromises()

    expect(saveContent).toHaveBeenCalled()
    expect(voiceInputButton(wrapper).attributes("aria-pressed")).toBe("true")
  })

  it("keeps the button unavailable from Stop until the last text has been added", async () => {
    await startRecording(wrapper)
    const finishConversion = holdConversion("hello")
    stopConvertsTheRest()
    await stopRecording(wrapper)

    const button = voiceInputButton(wrapper)
    expect(button.attributes("aria-label")).toBe("Voice input")
    expect(button.attributes()).toHaveProperty("disabled")
    expect(button.attributes()).not.toHaveProperty("aria-pressed")
    expect(button.classes()).not.toContain("daisy-btn-primary")
    await button.trigger("click")
    expect(recorder().startRecording).toHaveBeenCalledTimes(1)
    expect(saveContent).not.toHaveBeenCalled()

    finishConversion()
    await flushPromises()
    expect(saveContent).toHaveBeenCalled()
    expectIdleVoiceInputButton(wrapper)
  })

  it("returns to idle, adding nothing, when Stop found no speech", async () => {
    await startRecording(wrapper)
    convertsTo([])
    stopConvertsTheRest()
    await stopRecording(wrapper)

    expectIdleVoiceInputButton(wrapper)
    expect(saveContent).not.toHaveBeenCalled()
    await noToastShown()
  })

  it("offers the idle button again when saving the dictated text fails", async () => {
    saveContent.mockResolvedValue(wrapSdkError({ message: "save failed" }))
    await startRecording(wrapper)
    convertsTo(["hello"])
    stopConvertsTheRest()
    await stopRecording(wrapper)

    expectIdleVoiceInputButton(wrapper)
  })
})
