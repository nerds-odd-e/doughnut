import { AiAudioController } from "@generated/donut-backend-api/sdk.gen"
import { createAudioRecorder } from "@/models/audio/audioRecorder"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import type { ComponentPublicInstance } from "vue"
import { mockSdkService } from "@tests/helpers"
import {
  clearAudioHardwareMocks,
  installAudioBrowserSpies,
} from "@tests/notes/noteAudioToolsMocks"
import { audioTextResponse } from "@tests/notes/noteAudioToolsTestSupport"
import {
  findNoteNewFormButtonByText,
  mountNoteNewForm,
  notebookRootProps,
  noteTitleText,
  setupNoteNewFormSdkMocks,
  speakTitleStatus,
} from "@tests/notes/noteNewFormTestSupport"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

vi.mock("@/models/audio/recorderWorklet", async () => {
  const { recorderWorkletMockExports } = await import(
    "@tests/notes/noteAudioToolsMocks"
  )
  return recorderWorkletMockExports()
})

vi.mock("@/models/audio/audioRecorder", async () => {
  const { audioRecorderInvokingCallbackMockExports } = await import(
    "@tests/notes/noteAudioToolsMocks"
  )
  return audioRecorderInvokingCallbackMockExports()
})

describe("NoteNewForm spoken title", () => {
  let wrapper: VueWrapper<ComponentPublicInstance>

  beforeEach(() => {
    vi.useFakeTimers()
    vi.resetAllMocks()
    setupNoteNewFormSdkMocks()
    installAudioBrowserSpies()
    clearAudioHardwareMocks()
    mockSdkService(
      AiAudioController,
      "audioToText",
      audioTextResponse("Photosynthesis in desert plants.")
    )
  })

  afterEach(() => {
    wrapper?.unmount()
    vi.restoreAllMocks()
    vi.runOnlyPendingTimers()
    vi.useRealTimers()
    document.body.innerHTML = ""
  })

  async function speakTheTitle() {
    await findNoteNewFormButtonByText(wrapper, "Speak the title")!.trigger(
      "click"
    )
    await flushPromises()
  }

  async function stopSpeaking() {
    await findNoteNewFormButtonByText(wrapper, "Stop")!.trigger("click")
    await flushPromises()
  }

  it("names the control in words when idle and while listening", async () => {
    wrapper = mountNoteNewForm(notebookRootProps, { attachTo: document.body })

    expect(findNoteNewFormButtonByText(wrapper, "Speak the title")).toBeTruthy()
    expect(findNoteNewFormButtonByText(wrapper, "Stop")).toBeUndefined()
    expect(speakTitleStatus(wrapper)).toBeUndefined()

    await speakTheTitle()

    expect(findNoteNewFormButtonByText(wrapper, "Stop")).toBeTruthy()
    expect(
      findNoteNewFormButtonByText(wrapper, "Speak the title")
    ).toBeUndefined()
    expect(speakTitleStatus(wrapper)).toBe("Recording. Speak now.")
    expect(vi.mocked(createAudioRecorder)).toHaveBeenCalledWith(
      expect.any(Function),
      { convertOnlyAtStop: true }
    )
  })

  it("announces converting, then puts heard words in the title and clears status", async () => {
    wrapper = mountNoteNewForm(notebookRootProps, { attachTo: document.body })
    await speakTheTitle()

    const recorder = vi.mocked(createAudioRecorder).mock.results[0]!.value as {
      stopRecording: ReturnType<typeof vi.fn>
    }
    const processAudio = vi.mocked(createAudioRecorder).mock.calls[0]![0]!
    let finishStop!: () => void
    recorder.stopRecording.mockImplementation(
      () =>
        new Promise<File>((resolve) => {
          finishStop = () => {
            processAudio({
              data: new File([], "test.webm"),
              isMidSpeech: false,
            }).then(() => resolve(new File([], "test.webm")))
          }
        })
    )

    const stopClick = stopSpeaking()
    await flushPromises()
    expect(speakTitleStatus(wrapper)).toBe("Turning your speech into text…")

    finishStop()
    await stopClick
    await flushPromises()

    expect(noteTitleText(wrapper)).toBe("Photosynthesis in desert plants.")
    expect(speakTitleStatus(wrapper)).toBeUndefined()
    expect(findNoteNewFormButtonByText(wrapper, "Speak the title")).toBeTruthy()
  })
})
