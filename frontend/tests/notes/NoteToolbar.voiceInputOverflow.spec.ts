import makeMe from "donut-test-fixtures/makeMe"
import {
  installMockResizeObserver,
  layoutNoteToolbar,
  overflowTogglesNavWidth,
  restoreNoteToolbarWidthMocks,
} from "@tests/helpers/mockNoteToolbarNavWidth"
import {
  noteMoreOptionsTitles,
  noteVoiceInputTitles,
} from "@/components/notes/widgets/noteMoreOptionsTitles"
import {
  mountNoteToolbar,
  noteToolbarAction,
  openNoteToolbarOverflowMenu,
  overflowMenuItem,
  resetNoteToolbarTestState,
} from "@tests/notes/noteToolbarTestHelpers"
import {
  AiAudioController,
  TextContentController,
} from "@generated/donut-backend-api/sdk.gen"
import { createAudioRecorder } from "@/models/audio/audioRecorder"
import { mockSdkService, wrapSdkError } from "@tests/helpers"
import { describe, it, expect, afterEach, beforeEach, vi } from "vitest"
import { type VueWrapper, flushPromises } from "@vue/test-utils"

const titles = noteMoreOptionsTitles

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

describe("NoteToolbar Voice input on a narrow toolbar", () => {
  // biome-ignore lint/suspicious/noExplicitAny: wrapper for testing
  let wrapper: VueWrapper<any>

  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ""
    restoreNoteToolbarWidthMocks()
    vi.unstubAllGlobals()
  })

  beforeEach(() => {
    installMockResizeObserver()
    resetNoteToolbarTestState()
  })

  it("pins Voice input on a narrow toolbar while recording then returns it to overflow when stopped", async () => {
    wrapper = await mountNoteToolbar(makeMe.aNoteRealm.please())
    await layoutNoteToolbar(wrapper, overflowTogglesNavWidth())

    await openNoteToolbarOverflowMenu(wrapper)
    overflowMenuItem(titles.voiceInput)!.click()
    await flushPromises()

    const stopButton = noteToolbarAction(wrapper, noteVoiceInputTitles.stop)
    expect(stopButton.isVisible()).toBe(true)
    expect(stopButton.attributes("aria-pressed")).toBe("true")
    expect(document.querySelector("[data-dropdown-portal-panel]")).toBeNull()

    await stopButton.trigger("click")
    await flushPromises()

    await openNoteToolbarOverflowMenu(wrapper)
    expect(overflowMenuItem(titles.voiceInput)).not.toBeNull()
  })

  describe("with a recording kept after a failed conversion at Stop", () => {
    let audioToText: ReturnType<typeof mockSdkService>

    beforeEach(async () => {
      audioToText = mockSdkService(AiAudioController, "audioToText", {
        segmentTexts: ["hello"],
        endTimestamp: "00:00:01,000",
      }).mockResolvedValueOnce(wrapSdkError("API Error"))
      mockSdkService(
        TextContentController,
        "updateNoteContent",
        makeMe.aNoteRealm.please()
      )
      wrapper = await mountNoteToolbar(makeMe.aNoteRealm.please())
      await layoutNoteToolbar(wrapper, overflowTogglesNavWidth())
      const recorderMock = vi.mocked(createAudioRecorder).mock
      const convert = recorderMock.calls.at(-1)![0]
      const recorder = vi.mocked(recorderMock.results.at(-1)!.value)
      recorder.hasUnconvertedAudio.mockReturnValue(true)
      recorder.stopRecording.mockImplementation(async () => {
        await convert({
          data: new File([], "test.webm"),
          isMidSpeech: false,
        }).catch(() => undefined)
      })

      await openNoteToolbarOverflowMenu(wrapper)
      overflowMenuItem(titles.voiceInput)!.click()
      await flushPromises()
      await noteToolbarAction(wrapper, noteVoiceInputTitles.stop).trigger(
        "click"
      )
      await flushPromises()
    })

    it("keeps the retry on a narrow toolbar until the recording is turned into text", async () => {
      const retryButton = noteToolbarAction(wrapper, noteVoiceInputTitles.retry)
      expect(retryButton.isVisible()).toBe(true)
      await openNoteToolbarOverflowMenu(wrapper)
      expect(overflowMenuItem(titles.voiceInput)).toBeNull()

      await retryButton.trigger("click")
      await flushPromises()

      expect(audioToText).toHaveBeenCalledTimes(2)
      expect(noteToolbarAction(wrapper, titles.voiceInput).isVisible()).toBe(
        false
      )
      await openNoteToolbarOverflowMenu(wrapper)
      expect(overflowMenuItem(titles.voiceInput)).not.toBeNull()
    })

    it("offers idle Voice input in the menu of the next note page", async () => {
      wrapper.unmount()
      document.body.innerHTML = ""

      wrapper = await mountNoteToolbar(makeMe.aNoteRealm.please())
      await layoutNoteToolbar(wrapper, overflowTogglesNavWidth())

      expect(noteToolbarAction(wrapper, titles.voiceInput).isVisible()).toBe(
        false
      )
      await openNoteToolbarOverflowMenu(wrapper)
      expect(overflowMenuItem(titles.voiceInput)).not.toBeNull()
    })
  })
})
