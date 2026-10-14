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
  audioTextResponse,
  mountNoteToolbarRecording,
  moveNoteToolbarTo,
} from "@tests/notes/noteVoiceInputButtonTestSupport"
import {
  AiAudioController,
  TextContentController,
} from "@generated/donut-backend-api/sdk.gen"
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

  it("returns Voice input to overflow when the author moves to another note while recording", async () => {
    wrapper = await mountNoteToolbar(makeMe.aNoteRealm.please())
    await layoutNoteToolbar(wrapper, overflowTogglesNavWidth())
    await openNoteToolbarOverflowMenu(wrapper)
    overflowMenuItem(titles.voiceInput)!.click()
    await flushPromises()

    await moveNoteToolbarTo(wrapper, makeMe.aNoteRealm.please())

    expect(noteToolbarAction(wrapper, noteVoiceInputTitles.stop).exists()).toBe(
      false
    )
    expect(noteToolbarAction(wrapper, titles.voiceInput).isVisible()).toBe(
      false
    )
    await openNoteToolbarOverflowMenu(wrapper)
    expect(overflowMenuItem(titles.voiceInput)).not.toBeNull()
  })

  it("keeps the next note's recording pinned when the left note's last speech is added after it started", async () => {
    const { wrapper: toolbar, recorder } = await mountNoteToolbarRecording(
      makeMe.aNoteRealm.please()
    )
    wrapper = toolbar
    await layoutNoteToolbar(wrapper, overflowTogglesNavWidth())
    let addLastSpeech!: () => void
    recorder.stopRecording.mockImplementation(
      () => new Promise<void>((resolve) => (addLastSpeech = resolve))
    )
    await moveNoteToolbarTo(wrapper, makeMe.aNoteRealm.please())
    await openNoteToolbarOverflowMenu(wrapper)
    overflowMenuItem(titles.voiceInput)!.click()
    await flushPromises()

    addLastSpeech()
    await flushPromises()

    expect(
      noteToolbarAction(wrapper, noteVoiceInputTitles.stop).isVisible()
    ).toBe(true)
  })

  describe("with a recording kept after a failed conversion at Stop", () => {
    let audioToText: ReturnType<typeof mockSdkService>

    beforeEach(async () => {
      audioToText = mockSdkService(
        AiAudioController,
        "audioToText",
        audioTextResponse("hello")
      ).mockResolvedValueOnce(wrapSdkError("API Error"))
      mockSdkService(
        TextContentController,
        "updateNoteContent",
        makeMe.aNoteRealm.please()
      )
      const recording = await mountNoteToolbarRecording(
        makeMe.aNoteRealm.please()
      )
      wrapper = recording.wrapper
      recording.recorder.hasUnconvertedAudio.mockReturnValue(true)
      await layoutNoteToolbar(wrapper, overflowTogglesNavWidth())
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
