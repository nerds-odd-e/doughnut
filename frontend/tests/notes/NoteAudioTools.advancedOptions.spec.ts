import { AiAudioController } from "@generated/doughnut-backend-api/sdk.gen"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
import {
  audioToolsVm,
  findButtonByTitle,
  mountNoteAudioTools,
  processAudio,
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

describe("NoteAudioTools advanced options", () => {
  let wrapper: NoteAudioToolsWrapper
  const note = makeMe.aNote.please()
  let audioToTextMock: ReturnType<typeof mockSdkService>

  beforeEach(() => {
    audioToTextMock = mockSdkService(AiAudioController, "audioToText", {
      completionFromAudio: { content: "text" },
      endTimestamp: "00:00:37,270",
    })
    wrapper = mountNoteAudioTools(note)
  })

  afterEach(() => {
    wrapper?.unmount()
  })

  it("toggles advanced options panel", async () => {
    const advancedButton = findButtonByTitle(wrapper, "Advanced Options")!

    expect(wrapper.find(".advanced-options").exists()).toBe(false)
    await advancedButton.trigger("click")
    expect(wrapper.find(".advanced-options").exists()).toBe(true)
    await advancedButton.trigger("click")
    expect(wrapper.find(".advanced-options").exists()).toBe(false)
  })

  it("includes processing instructions in audio API calls", async () => {
    await findButtonByTitle(wrapper, "Advanced Options")!.trigger("click")
    await wrapper.find("#processingInstructions").setValue("Test instructions")

    await processAudio(wrapper)

    expect(audioToTextMock).toHaveBeenCalledWith({
      body: expect.objectContaining({
        additionalProcessingInstructions: "Test instructions",
        previousNoteContentToAppendTo: note.content,
      }),
    })
  })

  it("keeps processing instructions across recordings", async () => {
    await findButtonByTitle(wrapper, "Advanced Options")!.trigger("click")
    await wrapper.find("#processingInstructions").setValue("Test instructions")

    await processAudio(wrapper)
    await processAudio(wrapper)

    expect(audioToTextMock.mock.calls.length).toBeGreaterThanOrEqual(2)
    expect(audioToTextMock.mock.calls[1]?.[0]).toMatchObject({
      body: {
        additionalProcessingInstructions: "Test instructions",
        previousNoteContentToAppendTo: note.content,
      },
    })
  })

  describe("fullscreen errors", () => {
    beforeEach(() => {
      vi.useRealTimers()
      document.body.innerHTML = ""

      Object.defineProperty(
        document.documentElement,
        "webkitRequestFullscreen",
        {
          get: () => undefined,
          configurable: true,
        }
      )
      Object.defineProperty(document, "webkitFullscreenElement", {
        get: () => undefined,
        configurable: true,
      })
      vi.spyOn(document.documentElement, "requestFullscreen").mockResolvedValue(
        undefined
      )
      vi.spyOn(document, "exitFullscreen").mockResolvedValue(undefined)
      vi.spyOn(document, "exitPointerLock")
      vi.spyOn(document.documentElement, "requestPointerLock")
      Object.defineProperty(document, "fullscreenElement", {
        configurable: true,
        get: () => document.documentElement,
      })
      Object.defineProperty(document, "pointerLockElement", {
        configurable: true,
        get: () => document.documentElement,
      })

      wrapper?.unmount()
      wrapper = mountNoteAudioTools(note)
    })

    afterEach(() => {
      vi.useFakeTimers()
    })

    it("shows FullScreen control in advanced options", async () => {
      await findButtonByTitle(wrapper, "Advanced Options")!.trigger("click")
      expect(wrapper.find(".fullscreen-btn").exists()).toBe(true)
    })

    it("displays error message in fullscreen overlay", async () => {
      await findButtonByTitle(wrapper, "Advanced Options")!.trigger("click")
      await flushPromises()

      audioToolsVm(wrapper).errors = { someError: "Test error message" }
      await flushPromises()

      await wrapper.find(".fullscreen-btn").trigger("click")
      await flushPromises()

      const errorElement = document.body.querySelector(
        ".fullscreen-overlay .fullscreen-error"
      )
      expect(errorElement?.textContent?.trim()).toBe("Test error message")
    })
  })
})
