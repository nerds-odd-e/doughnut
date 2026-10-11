/// <reference types="@vitest/browser-playwright" />
import {
  AiAudioController,
  NoteController,
  TextContentController,
} from "@generated/donut-backend-api/sdk.gen"
import NoteShow from "@/components/notes/NoteShow.vue"
import { createAudioRecorder } from "@/models/audio/audioRecorder"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import helper, {
  mockSdkService,
  mockSdkServiceWithImplementation,
  wrapSdkResponse,
} from "@tests/helpers"
import { noteTitleText } from "@tests/notes/noteNewFormTestSupport"
import {
  placeCaretInTitle,
  microphoneCannotStart,
  mockAudioToTextFailThen,
  mockAudioToTextWithNoSegments,
  hearing,
  titleEditable,
  useSpokenTitleTestLifecycle,
} from "@tests/notes/spokenTitleTestSupport"
import {
  editTitle,
  titleEditorEl,
  mockUpdateNoteTitle,
  mockedUpdateTitleCall,
} from "@tests/notes/noteTextContentTestSupport"
import {
  audioTextResponse,
  startVoiceInputByPointer,
  stopVoiceInputWithFocus,
} from "@tests/notes/noteVoiceInputButtonTestSupport"
import { dictationMarker } from "@tests/notes/dictationMarkerTestSupport"
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
vi.mock("@/models/wakeLocker", async () => {
  const { wakeLockerMockExports } = await import(
    "@tests/notes/noteVoiceInputButtonMocks"
  )
  return wakeLockerMockExports()
})
useSpokenTitleTestLifecycle("harvest")

import { cdp } from "vitest/browser"
import {
  flushReferencedTitleBlurDiscardCheck,
  referencedTitleSaveKeepVisibleTextSelector,
} from "@tests/notes/textContentWrapperTestSupport"
import {
  showToastsOnPage,
  toastMessage,
  toastShown,
} from "@tests/helpers/toastTestSupport"
showToastsOnPage()
describe("Toolbar voice input with a referenced title draft", () => {
  let wrapper: VueWrapper
  beforeEach(async () => {
    const realm = makeMe.aNoteRealm.title("Orchard notes").please()
    makeMe.aNoteRealm.under(realm).please()
    mockSdkService(NoteController, "showNote", realm)
    mockUpdateNoteTitle()
    mockSdkServiceWithImplementation(
      TextContentController,
      "updateNoteContent",
      async (options) =>
        makeMe.aNoteRealm
          .title("Orchard notes")
          .content(options.body!.content)
          .please()
    )
    wrapper = helper
      .component(NoteShow)
      .withRouter()
      .withCleanStorage()
      .withCurrentUser(makeMe.aUser.please())
      .withProps({ noteId: realm.id })
      .mount({ attachTo: document.body })
    await flushPromises()
    titleEditorEl(wrapper).focus()
    await editTitle(wrapper, "Spring orchard notes")
    await flushPromises()
    placeCaretInTitle(wrapper, 0, 6)
  })
  afterEach(() => wrapper.unmount())

  const start = () => startVoiceInputByPointer(wrapper)
  const stop = () => stopVoiceInputWithFocus(wrapper)

  it("keeps the draft while conversion waits, then saves the composed title with an explicit choice", async () => {
    let finish!: () => void
    mockSdkService(
      AiAudioController,
      "audioToText",
      audioTextResponse("Autumn")
    ).mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          finish = () => resolve(wrapSdkResponse(audioTextResponse("Autumn")))
        })
    )
    await start()
    const listening = wrapper.find('button[aria-label="Stop voice input"]')
    expect(listening.attributes("aria-pressed")).toBe("true")
    expect(dictationMarker()!.getAttribute("aria-hidden")).toBe("true")
    await stop()
    await flushReferencedTitleBlurDiscardCheck()
    expect(noteTitleText(wrapper)).toBe("Spring orchard notes")
    expect(titleEditable(wrapper)).toBe("false")
    expect(
      wrapper.find('button[aria-label="Voice input"]').attributes()
    ).toHaveProperty("disabled")
    finish()
    await flushPromises()
    expect(noteTitleText(wrapper)).toBe("Autumn orchard notes")
    await wrapper
      .find(referencedTitleSaveKeepVisibleTextSelector)
      .trigger("click")
    expect(mockedUpdateTitleCall).toHaveBeenCalledWith(
      expect.objectContaining({
        body: {
          newTitle: "Autumn orchard notes",
          referenceHandling: "KEEP_VISIBLE_TEXT",
        },
      })
    )
  })

  it.each(["microphone", "silent", "empty", "conversion"])(
    "restores the draft and permits fresh recording after %s",
    async (failure) => {
      if (failure === "microphone") microphoneCannotStart()
      if (failure === "empty") mockAudioToTextWithNoSegments()
      if (failure === "conversion") mockAudioToTextFailThen("Autumn")
      await start()
      if (failure !== "microphone") {
        if (failure === "silent") {
          vi.mocked(createAudioRecorder)
            .mock.results.at(-1)!
            .value.stopRecording.mockResolvedValue(undefined)
        }
        await stop()
      }
      await flushReferencedTitleBlurDiscardCheck()
      expect(noteTitleText(wrapper)).toBe("Spring orchard notes")
      expect(titleEditable(wrapper)).toBe("true")
      if (failure === "microphone" || failure === "conversion") {
        expect(toastMessage(await toastShown("error"))).toBe(
          failure === "microphone"
            ? "Could not use the microphone. Allow microphone access in your browser, then try again."
            : "Could not turn your speech into text."
        )
      }
      hearing("Autumn")
      await start()
      await stop()
      expect(noteTitleText(wrapper)).toBe("Autumn orchard notes")
    }
  )

  it("discards the unsaved composed draft on an ordinary departure", async () => {
    hearing("Autumn")
    await start()
    await stop()
    const content = wrapper.find('.note-content [contenteditable="true"]')
      .element as HTMLElement
    content.focus()
    await flushReferencedTitleBlurDiscardCheck()
    expect(noteTitleText(wrapper)).toBe("Orchard notes")
  })

  it("keeps the pending marker still for reduced motion", async () => {
    await cdp().send("Emulation.setEmulatedMedia", {
      features: [{ name: "prefers-reduced-motion", value: "reduce" }],
    })
    try {
      await start()
      expect(
        [...dictationMarker()!.children].map(
          (dot) => getComputedStyle(dot).animationName
        )
      ).toEqual(["none", "none", "none"])
    } finally {
      await cdp().send("Emulation.setEmulatedMedia", {
        features: [{ name: "prefers-reduced-motion", value: "" }],
      })
    }
  })
})
