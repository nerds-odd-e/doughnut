import { AiAudioController } from "@generated/donut-backend-api/sdk.gen"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
import {
  audioToolsVm,
  findButtonByTitle,
  mountNoteAudioTools,
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

  beforeEach(() => {
    mockSdkService(AiAudioController, "audioToText", {
      segmentTexts: ["text"],
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

    it("offers full-screen editing as the only advanced option", async () => {
      await findButtonByTitle(wrapper, "Advanced Options")!.trigger("click")
      const controls = wrapper
        .find(".advanced-options")
        .findAll("button, input, select, textarea")
      expect(controls.map((control) => control.attributes("title"))).toEqual([
        "Toggle Full Screen",
      ])
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
