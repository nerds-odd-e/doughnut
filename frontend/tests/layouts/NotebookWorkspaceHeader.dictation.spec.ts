import {
  AiAudioController,
  TextContentController,
} from "@generated/donut-backend-api/sdk.gen"
import {
  noteMoreOptionsTitles,
  noteVoiceInputTitles,
} from "@/components/notes/widgets/noteMoreOptionsTitles"
import { createAudioRecorder } from "@/models/audio/audioRecorder"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import {
  mockSdkService,
  mockSdkServiceWithImplementation,
  wrapSdkError,
  wrapSdkResponse,
} from "@tests/helpers"
import { noteTitleText } from "@tests/notes/noteNewFormTestSupport"
import {
  hearing,
  holdMicrophoneStartup,
  placeCaretInTitle,
  useSpokenTitleTestLifecycle,
} from "@tests/notes/spokenTitleTestSupport"
import {
  mockUpdateNoteTitle,
  mockedUpdateTitleCall,
} from "@tests/notes/noteTextContentTestSupport"
import {
  audioTextResponse,
  startVoiceInputByPointer,
  stopVoiceInputWithFocus,
} from "@tests/notes/noteVoiceInputButtonTestSupport"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { page } from "vitest/browser"
import {
  atViewportWidth,
  chooseFromMore,
  clickPageBarCommand,
  commandsSitInPageBar,
  mountNotebookWorkspace,
  pageBarCommand,
} from "./notebookWorkspaceHeaderTestSupport"

const titles = noteMoreOptionsTitles

vi.mock("@/models/audio/recorderWorklet", async () => {
  const { recorderWorkletMockExports } = await import(
    "@tests/notes/noteVoiceInputButtonMocks"
  )
  return recorderWorkletMockExports()
})
vi.mock("@/models/audio/audioRecorder", async () => {
  const { audioRecorderInvokingCallbackMockExports } = await import(
    "@tests/notes/noteVoiceInputButtonMocks"
  )
  return audioRecorderInvokingCallbackMockExports()
})
vi.mock("@/models/wakeLocker", async () => {
  const { wakeLockerMockExports } = await import(
    "@tests/notes/noteVoiceInputButtonMocks"
  )
  return wakeLockerMockExports()
})
useSpokenTitleTestLifecycle("harvest")

describe("Notebook workspace header dictation", () => {
  let wrapper: VueWrapper
  const saveContent = vi.fn()

  beforeEach(async () => {
    mockUpdateNoteTitle()
    mockSdkServiceWithImplementation(
      TextContentController,
      "updateNoteContent",
      async (options) => {
        saveContent(options.body!.content)
        return makeMe.aNoteRealm
          .title("Orchard notes")
          .content(options.body!.content)
          .please()
      }
    )
  })

  async function mountAt(width: number) {
    await atViewportWidth(width)
    ;({ wrapper } = await mountNotebookWorkspace([
      makeMe.aNoteRealm.title("Orchard notes").please(),
    ]))
  }

  afterEach(() => {
    wrapper.unmount()
    document.body.innerHTML = ""
  })

  async function settle() {
    await vi.runOnlyPendingTimersAsync()
    await flushPromises()
  }

  it("dictates into the title caret from the bar while narrowing, then the next recording goes to the body", async () => {
    await mountAt(1280)
    placeCaretInTitle(wrapper, 7)
    await startVoiceInputByPointer(wrapper)

    await page.viewport(320, 700)
    await vi.waitFor(() => {
      expect(pageBarCommand(noteVoiceInputTitles.stop)).not.toBeNull()
      expect(commandsSitInPageBar(320)).toBe(true)
    })

    await stopVoiceInputWithFocus(wrapper)
    expect(noteTitleText(wrapper)).toBe("Orchard harvest notes")
    await settle()
    expect(mockedUpdateTitleCall).toHaveBeenCalledWith(
      expect.objectContaining({
        body: expect.objectContaining({ newTitle: "Orchard harvest notes" }),
      })
    )

    ;(document.activeElement as HTMLElement | null)?.blur()
    hearing("pruned trees")
    await startVoiceInputByPointer(wrapper)
    await stopVoiceInputWithFocus(wrapper)
    await settle()
    expect(saveContent).toHaveBeenCalledWith(
      expect.stringContaining("pruned trees")
    )
    expect(noteTitleText(wrapper)).toBe("Orchard harvest notes")
  })

  describe("at phone width", () => {
    async function mountAtPhoneWidth() {
      await mountAt(320)
      await vi.waitFor(() =>
        expect(pageBarCommand(titles.overflowMenu)).not.toBeNull()
      )
    }

    function expectBusyVoiceInputInBar() {
      const button = pageBarCommand(titles.voiceInput)!
      expect(button.hasAttribute("disabled")).toBe(true)
      expect(button.querySelector(".animate-spin")).not.toBeNull()
      expect(commandsSitInPageBar(320)).toBe(true)
    }

    it("keeps the starting microphone in the bar, then its Stop works from there", async () => {
      const { finishStartup } = holdMicrophoneStartup()
      await mountAtPhoneWidth()
      await chooseFromMore(titles.voiceInput)
      await vi.waitFor(() => expectBusyVoiceInputInBar())

      finishStartup()
      await flushPromises()
      await vi.waitFor(() =>
        expect(pageBarCommand(noteVoiceInputTitles.stop)).not.toBeNull()
      )
      expect(commandsSitInPageBar(320)).toBe(true)
      await clickPageBarCommand(noteVoiceInputTitles.stop)
      await settle()
      expect(saveContent).toHaveBeenCalledWith(
        expect.stringContaining("harvest")
      )
    })

    it("keeps the converting voice input in the bar until the text arrives", async () => {
      let finish!: () => void
      mockSdkService(
        AiAudioController,
        "audioToText",
        audioTextResponse("harvest")
      ).mockImplementationOnce(
        () =>
          new Promise((resolve) => {
            finish = () =>
              resolve(wrapSdkResponse(audioTextResponse("harvest")))
          })
      )
      await mountAtPhoneWidth()
      await chooseFromMore(titles.voiceInput)
      await clickPageBarCommand(noteVoiceInputTitles.stop)
      await vi.waitFor(() => expectBusyVoiceInputInBar())

      finish()
      await settle()
      expect(
        pageBarCommand(titles.voiceInput)?.hasAttribute("disabled")
      ).not.toBe(true)
      expect(saveContent).toHaveBeenCalledWith(
        expect.stringContaining("harvest")
      )
    })

    it("offers Retry in the bar after a failed conversion and turns the speech into text from there", async () => {
      const audioToText = mockSdkService(
        AiAudioController,
        "audioToText",
        audioTextResponse("harvest")
      ).mockResolvedValueOnce(wrapSdkError("API Error"))
      await mountAtPhoneWidth()
      await chooseFromMore(titles.voiceInput)
      const recorder = vi.mocked(createAudioRecorder).mock.results.at(-1)!.value
      recorder.hasUnconvertedAudio.mockReturnValue(true)
      // Like the real recorder, a failed conversion keeps the audio
      // instead of failing Stop.
      const stopping = recorder.stopRecording.getMockImplementation()!
      recorder.stopRecording.mockImplementation(() =>
        stopping().catch(() => undefined)
      )
      await clickPageBarCommand(noteVoiceInputTitles.stop)
      await vi.waitFor(() => {
        expect(pageBarCommand(noteVoiceInputTitles.retry)).not.toBeNull()
        expect(commandsSitInPageBar(320)).toBe(true)
      })

      await clickPageBarCommand(noteVoiceInputTitles.retry)
      await settle()
      expect(audioToText).toHaveBeenCalledTimes(2)
      expect(saveContent).toHaveBeenCalledWith(
        expect.stringContaining("harvest")
      )
    })

    it("fits an open Assimilate, a recording Stop, and More in the bar together", async () => {
      await mountAtPhoneWidth()
      await chooseFromMore(titles.assimilation)
      await chooseFromMore(titles.voiceInput)

      await vi.waitFor(() => {
        expect(
          pageBarCommand(titles.assimilation)?.getAttribute("aria-pressed")
        ).toBe("true")
        expect(
          pageBarCommand(noteVoiceInputTitles.stop)?.getAttribute(
            "aria-pressed"
          )
        ).toBe("true")
        expect(pageBarCommand(titles.overflowMenu)).not.toBeNull()
        expect(commandsSitInPageBar(320)).toBe(true)
      })
    })
  })
})
