import {
  AiAudioController,
  TextContentController,
} from "@generated/donut-backend-api/sdk.gen"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
import { useNoteStore } from "@/store/noteStore"
import {
  audioTextResponse,
  audioToolsVm,
  dictationStatus,
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

describe("NoteAudioTools dictation status", () => {
  let wrapper: NoteAudioToolsWrapper
  const realm = makeMe.aNoteRealm.please()

  beforeEach(() => {
    wrapper = mountNoteAudioTools(realm.note)
  })

  afterEach(() => {
    wrapper?.unmount()
  })

  it("says it is turning speech into text until Stop has finished", async () => {
    let finishStop!: () => void
    await startRecording(wrapper)
    audioToolsVm(wrapper).audioRecorder.stopRecording.mockImplementation(
      () => new Promise<void>((resolve) => (finishStop = resolve))
    )

    await stopRecording(wrapper)
    expect(dictationStatus(wrapper)).toBe("Turning your speech into text…")

    finishStop()
    await flushPromises()
    expect(dictationStatus(wrapper)).toBe("No speech was turned into text.")
    expect(findButtonByText(wrapper, "Record")).toBeTruthy()
  })

  it("explains a microphone that cannot be used until a later Record starts", async () => {
    audioToolsVm(wrapper).audioRecorder.startRecording.mockRejectedValueOnce(
      new Error("Permission denied")
    )
    await startRecording(wrapper)

    const status = wrapper.get('[role="status"]')
    expect(status.text()).toBe(
      "Could not use the microphone. Allow microphone access in your browser, then try again."
    )
    expect(status.classes()).toContain("text-error")
    expect(findButtonByText(wrapper, "Record")).toBeTruthy()
    expect(findButtonByText(wrapper, "Stop")).toBeUndefined()

    await startRecording(wrapper)
    expect(dictationStatus(wrapper)).toBe("Recording. Speak now.")
    expect(wrapper.get('[role="status"]').classes()).not.toContain("text-error")
  })

  describe("after Stop", () => {
    const stopConverting = (segmentTexts: string[]) => {
      audioToolsVm(wrapper).audioRecorder.stopRecording.mockImplementation(
        async () => {
          await processAudio(wrapper)
        }
      )
      mockSdkService(AiAudioController, "audioToText", {
        segmentTexts,
        endTimestamp: "00:00:37,270",
      })
    }

    let saveContent: ReturnType<typeof mockSdkService>

    beforeEach(() => {
      useNoteStore().refreshNoteRealm(realm)
      saveContent = mockSdkService(
        TextContentController,
        "updateNoteContent",
        makeMe.aNoteRealm.please()
      )
    })

    it("says text was added when a passage was written", async () => {
      await startRecording(wrapper)
      stopConverting(["hello"])
      await stopRecording(wrapper)
      expect(dictationStatus(wrapper)).toBe("Added to your note.")
    })

    it("says no speech was turned into text when the conversion returned none", async () => {
      await startRecording(wrapper)
      stopConverting([])
      await stopRecording(wrapper)
      expect(dictationStatus(wrapper)).toBe("No speech was turned into text.")
      expect(saveContent).not.toHaveBeenCalled()
    })

    it("tells the result of its recording when Record is pressed while Stop finishes", async () => {
      mockSdkService(
        AiAudioController,
        "audioToText",
        audioTextResponse("hello")
      )
      await startRecording(wrapper)
      await processAudio(wrapper, midSpeechChunk())
      await flushPromises()
      const recorder = audioToolsVm(wrapper).audioRecorder
      let finishStop!: () => void
      recorder.stopRecording.mockImplementation(
        () => new Promise<void>((resolve) => (finishStop = resolve))
      )
      await stopRecording(wrapper)

      expect(findButtonByText(wrapper, "Record")!.attributes()).toHaveProperty(
        "disabled"
      )
      await startRecording(wrapper)
      expect(dictationStatus(wrapper)).toBe("Turning your speech into text…")
      expect(recorder.startRecording).toHaveBeenCalledTimes(1)

      finishStop()
      await flushPromises()
      expect(dictationStatus(wrapper)).toBe("Added to your note.")
      expect(
        findButtonByText(wrapper, "Record")!.attributes()
      ).not.toHaveProperty("disabled")
    })

    it("starts a new recording with nothing counted", async () => {
      await startRecording(wrapper)
      stopConverting(["hello"])
      await stopRecording(wrapper)

      await startRecording(wrapper)
      expect(dictationStatus(wrapper)).toBe("Recording. Speak now.")
      stopConverting([])
      await stopRecording(wrapper)
      expect(dictationStatus(wrapper)).toBe("No speech was turned into text.")
    })
  })

  it("keeps recording status and Stop when a mid-speech conversion finishes", async () => {
    useNoteStore().refreshNoteRealm(realm)
    mockSdkService(AiAudioController, "audioToText", audioTextResponse("test"))
    const saveContent = mockSdkService(
      TextContentController,
      "updateNoteContent",
      makeMe.aNoteRealm.please()
    )
    await startRecording(wrapper)

    await processAudio(wrapper, midSpeechChunk())
    await flushPromises()

    expect(saveContent).toHaveBeenCalled()

    expect(dictationStatus(wrapper)).toBe("Recording. Speak now.")
    expect(findButtonByText(wrapper, "Stop")).toBeTruthy()
    expect(findButtonByText(wrapper, "Record")).toBeUndefined()
  })
})
