import { createAudioRecorder } from "@/models/audio/audioRecorder"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import type { ComponentPublicInstance } from "vue"
import { wrapSdkResponse } from "@tests/helpers"
import {
  isNoteNewFormSubmitDisabled,
  mountNoteNewForm,
  notebookRootProps,
  noteNewFormNote,
  noteTitleText,
  setNoteNewFormTitle,
  setupNoteNewFormSdkMocks,
  type NoteNewFormSdkSpies,
} from "@tests/notes/noteNewFormTestSupport"
import {
  findSpeakTitleButtonByText,
  hearing,
  holdSpeakTitleConvertingUntilFinished,
  speakAndStop,
  speakTheTitle,
  speakTitleStatus,
  stopSpeaking,
  useSpokenTitleTestLifecycle,
} from "@tests/notes/spokenTitleTestSupport"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

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

useSpokenTitleTestLifecycle()

describe("NoteNewForm spoken title", () => {
  let wrapper: VueWrapper<ComponentPublicInstance>
  let sdkSpies: NoteNewFormSdkSpies

  beforeEach(() => {
    sdkSpies = setupNoteNewFormSdkMocks()
  })

  afterEach(() => {
    wrapper?.unmount()
  })

  it("names the control in words when idle and while listening", async () => {
    wrapper = mountNoteNewForm(notebookRootProps, { attachTo: document.body })

    expect(findSpeakTitleButtonByText(wrapper, "Speak the title")).toBeTruthy()
    expect(findSpeakTitleButtonByText(wrapper, "Stop")).toBeUndefined()
    expect(speakTitleStatus(wrapper)).toBeUndefined()

    await speakTheTitle(wrapper)

    expect(findSpeakTitleButtonByText(wrapper, "Stop")).toBeTruthy()
    expect(
      findSpeakTitleButtonByText(wrapper, "Speak the title")
    ).toBeUndefined()
    expect(speakTitleStatus(wrapper)).toBe("Recording. Speak now.")
    expect(vi.mocked(createAudioRecorder)).toHaveBeenCalledWith(
      expect.any(Function),
      { convertOnlyAtStop: true }
    )
  })

  it("announces converting, then puts heard words in the title and clears status", async () => {
    wrapper = mountNoteNewForm(notebookRootProps, { attachTo: document.body })
    await speakTheTitle(wrapper)

    const { finishStop } = holdSpeakTitleConvertingUntilFinished()

    const stopClick = stopSpeaking(wrapper)
    await flushPromises()
    expect(speakTitleStatus(wrapper)).toBe("Turning your speech into text…")

    finishStop()
    await stopClick
    await flushPromises()

    expect(noteTitleText(wrapper)).toBe("Photosynthesis in desert plants.")
    expect(speakTitleStatus(wrapper)).toBeUndefined()
    expect(findSpeakTitleButtonByText(wrapper, "Speak the title")).toBeTruthy()
  })

  it("joins heard segments onto a title pattern", async () => {
    hearing("weekly review")
    wrapper = mountNoteNewForm(
      { ...notebookRootProps, initialTitle: "2026-10-06" },
      { attachTo: document.body }
    )

    await speakAndStop(wrapper)

    expect(noteTitleText(wrapper)).toBe("2026-10-06 weekly review")
  })

  it("joins heard segments onto a typed title with one space", async () => {
    hearing("weekly review")
    wrapper = mountNoteNewForm(notebookRootProps, { attachTo: document.body })
    await setNoteNewFormTitle(wrapper, "Project")

    await speakAndStop(wrapper)

    expect(noteTitleText(wrapper)).toBe("Project weekly review")
  })

  it("replaces illegal path characters and shows the warning for heard segments", async () => {
    hearing("a/b")
    wrapper = mountNoteNewForm(notebookRootProps, { attachTo: document.body })

    await speakAndStop(wrapper)

    expect(noteTitleText(wrapper)).toBe("a／b")
    expect(wrapper.text()).toContain(
      "'/' is not a legal name, and it has been replaced with the fullwidth '／'"
    )
  })

  it("searches for existing notes with the heard title", async () => {
    sdkSpies.searchForRelationshipTargetWithinSpy.mockResolvedValue(
      wrapSdkResponse([
        {
          hitKind: "NOTE",
          noteSearchResult: {
            noteTopology: noteNewFormNote.noteTopology,
            notebookId: 1,
            distance: 0.9,
          },
        },
      ])
    )
    wrapper = mountNoteNewForm(notebookRootProps, { attachTo: document.body })

    await speakAndStop(wrapper)
    vi.runOnlyPendingTimers()
    await flushPromises()

    expect(sdkSpies.searchForRelationshipTargetWithinSpy).toHaveBeenCalledWith({
      path: { note: noteNewFormNote.id },
      body: expect.objectContaining({
        searchKey: "Photosynthesis in desert plants.",
      }),
    })
    expect(wrapper.text()).toContain("mythical")
  })

  it("submits the title after typing a correction to the heard words", async () => {
    wrapper = mountNoteNewForm(notebookRootProps, { attachTo: document.body })

    await speakAndStop(wrapper)
    await setNoteNewFormTitle(wrapper, "Corrected title")
    vi.clearAllTimers()

    await wrapper.find('[data-testid="note-new-form"]').trigger("submit")
    await flushPromises()

    expect(sdkSpies.mockedCreateNoteAtRoot).toHaveBeenCalledWith({
      path: { notebook: notebookRootProps.notebookId },
      body: expect.objectContaining({ newTitle: "Corrected title" }),
    })
  })

  it("does not offer Submit while listening or converting, and offers it once words are in", async () => {
    wrapper = mountNoteNewForm(notebookRootProps, { attachTo: document.body })
    expect(isNoteNewFormSubmitDisabled(wrapper)).toBe(false)

    await speakTheTitle(wrapper)
    expect(isNoteNewFormSubmitDisabled(wrapper)).toBe(true)

    const { finishStop } = holdSpeakTitleConvertingUntilFinished()
    const stopClick = stopSpeaking(wrapper)
    await flushPromises()
    expect(isNoteNewFormSubmitDisabled(wrapper)).toBe(true)

    finishStop()
    await stopClick
    await flushPromises()

    expect(isNoteNewFormSubmitDisabled(wrapper)).toBe(false)
  })

  it("does not create a note when Enter is pressed in the title while listening", async () => {
    wrapper = mountNoteNewForm(notebookRootProps, { attachTo: document.body })
    await speakTheTitle(wrapper)

    await wrapper.find('[data-test="note-title"]').trigger("keydown.enter")
    await flushPromises()

    expect(sdkSpies.mockedCreateNoteAtRoot).not.toHaveBeenCalled()
  })

  it("stops the recorder when the form unmounts while listening", async () => {
    wrapper = mountNoteNewForm(notebookRootProps, { attachTo: document.body })
    await speakTheTitle(wrapper)

    const recorder = vi.mocked(createAudioRecorder).mock.results[0]!.value as {
      stopRecording: ReturnType<typeof vi.fn>
    }
    expect(recorder.stopRecording).not.toHaveBeenCalled()

    wrapper.unmount()
    await flushPromises()

    expect(recorder.stopRecording).toHaveBeenCalled()
  })
})
