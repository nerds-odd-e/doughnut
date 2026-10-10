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
  audioToolsVm,
  findButtonByText,
  midSpeechChunk,
  mountNoteAudioTools,
  processAudio,
  startRecording,
  stopRecording,
  useNoteAudioToolsTestLifecycle,
  type NoteAudioToolsWrapper,
} from "@tests/notes/noteAudioToolsTestSupport"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

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

describe("NoteAudioTools controls during and after a conversion", () => {
  let wrapper: NoteAudioToolsWrapper
  let saveContent: ReturnType<typeof mockSdkService>
  const realm = makeMe.aNoteRealm.please()

  beforeEach(() => {
    wrapper = mountNoteAudioTools(realm.note)
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

  const buttonTexts = () =>
    wrapper.findAll("button").map((button) => button.text())
  const recorder = () => audioToolsVm(wrapper).audioRecorder
  const recordButton = () => findButtonByText(wrapper, "Record")!
  const writeNowButton = () => findButtonByText(wrapper, "Write text now")!

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

  it("disables Write text now during a conversion", async () => {
    await startRecording(wrapper)
    const finishConversion = holdConversion("test")

    const processing = processAudio(wrapper, midSpeechChunk())
    await flushPromises()
    expect(writeNowButton().attributes()).toHaveProperty("disabled")

    finishConversion()
    await processing
    await flushPromises()
    expect(writeNowButton().attributes()).not.toHaveProperty("disabled")
  })

  it("keeps Stop and Write text now when a mid-speech conversion finishes", async () => {
    convertsTo(["test"])
    await startRecording(wrapper)

    await processAudio(wrapper, midSpeechChunk())
    await flushPromises()

    expect(saveContent).toHaveBeenCalled()
    expect(buttonTexts()).toEqual(["Stop", "Write text now"])
  })

  it("keeps Record unavailable from Stop until the last text has been added", async () => {
    await startRecording(wrapper)
    const finishConversion = holdConversion("hello")
    stopConvertsTheRest()
    await stopRecording(wrapper)

    expect(buttonTexts()).toEqual(["Record"])
    expect(recordButton().attributes()).toHaveProperty("disabled")
    await startRecording(wrapper)
    expect(recorder().startRecording).toHaveBeenCalledTimes(1)
    expect(saveContent).not.toHaveBeenCalled()

    finishConversion()
    await flushPromises()
    expect(saveContent).toHaveBeenCalled()
    expect(recordButton().attributes()).not.toHaveProperty("disabled")
  })

  it("returns to Record alone, adding nothing, when Stop found no speech", async () => {
    await startRecording(wrapper)
    convertsTo([])
    stopConvertsTheRest()
    await stopRecording(wrapper)

    expect(wrapper.text()).toBe("Record")
    expect(recordButton().attributes()).not.toHaveProperty("disabled")
    expect(saveContent).not.toHaveBeenCalled()
  })

  it("offers Record again when saving the dictated text fails", async () => {
    saveContent.mockResolvedValue(wrapSdkError({ message: "save failed" }))
    await startRecording(wrapper)
    convertsTo(["hello"])
    stopConvertsTheRest()
    await stopRecording(wrapper)

    expect(saveContent).toHaveBeenCalled()
    expect(recordButton().attributes()).not.toHaveProperty("disabled")
  })
})
