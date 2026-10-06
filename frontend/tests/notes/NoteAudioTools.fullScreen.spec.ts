import { AiAudioController } from "@generated/donut-backend-api/sdk.gen"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
import {
  audioToolsVm,
  findButtonByText,
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

describe("NoteAudioTools full screen", () => {
  let wrapper: NoteAudioToolsWrapper
  const note = makeMe.aNote.please()

  beforeEach(() => {
    vi.useRealTimers()
    mockSdkService(AiAudioController, "audioToText", {
      segmentTexts: ["text"],
      endTimestamp: "00:00:37,270",
    })
    Object.defineProperty(document.documentElement, "webkitRequestFullscreen", {
      get: () => undefined,
      configurable: true,
    })
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
    wrapper = mountNoteAudioTools(note)
  })

  afterEach(() => {
    wrapper?.unmount()
  })

  it("displays the current error in the full-screen overlay", async () => {
    audioToolsVm(wrapper).errors = { someError: "Test error message" }
    await flushPromises()

    await findButtonByText(wrapper, "Full screen")!.trigger("click")
    await flushPromises()

    const errorElement = document.body.querySelector(
      ".fullscreen-overlay .fullscreen-error"
    )
    expect(errorElement?.textContent?.trim()).toBe("Test error message")
  })
})
