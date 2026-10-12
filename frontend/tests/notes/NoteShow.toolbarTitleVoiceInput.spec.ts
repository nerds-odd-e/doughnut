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
  selectWholeTitle,
  hearing,
  titleEditable,
  titleCaretOffset,
  expectTitleMarkerAfter,
  useSpokenTitleTestLifecycle,
} from "@tests/notes/spokenTitleTestSupport"
import {
  mockUpdateNoteTitle,
  mockedUpdateTitleCall,
  titleEditorEl,
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

describe("Toolbar voice input with a focused title", () => {
  let wrapper: VueWrapper
  beforeEach(async () => {
    const realm = makeMe.aNoteRealm.title("Orchard notes").please()
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
  })
  afterEach(() => wrapper.unmount())

  const start = () => startVoiceInputByPointer(wrapper)
  const stop = () => stopVoiceInputWithFocus(wrapper)

  it("inserts at the title caret, saves normally and restores editing after Stop", async () => {
    placeCaretInTitle(wrapper, 7)
    await start()
    expect(titleEditable(wrapper)).toBe("false")
    expectTitleMarkerAfter(wrapper, 7)
    await stop()
    expect(noteTitleText(wrapper)).toBe("Orchard harvest notes")
    expect(titleEditable(wrapper)).toBe("true")
    expect(document.activeElement).toBe(titleEditorEl(wrapper))
    expect(titleCaretOffset()).toBe("Orchard harvest".length)
    await vi.runOnlyPendingTimersAsync()
    await flushPromises()
    expect(mockedUpdateTitleCall).toHaveBeenCalledWith(
      expect.objectContaining({
        body: expect.objectContaining({
          newTitle: "Orchard harvest notes",
        }),
      })
    )
  })

  it("draws the title recording's level in the highlighted Stop button", async () => {
    placeCaretInTitle(wrapper, 7)
    await start()
    const titleRecorder = vi
      .mocked(createAudioRecorder)
      .mock.results.at(-1)!.value
    const button = wrapper.find('button[aria-label="Stop voice input"]')
    expect(button.attributes("aria-pressed")).toBe("true")
    expect(button.find("canvas").exists()).toBe(true)
    expect(titleRecorder.getAudioData).toHaveBeenCalled()
  })

  it.each([
    ["Where do migratory birds sleep?", "Where do migratory birds sleep？"],
    [
      "Managing a community orchard through late spring frosts while preserving pollinator habitat and the volunteers’ harvest records",
      "Managing a community orchard through late spring frosts while preserving pollinator habitat and the volunteers’ harvest records",
    ],
  ])(
    "requests title guidance and saves the returned title with ordinary name handling: %s",
    async (title, savedTitle) => {
      hearing(title)
      selectWholeTitle(wrapper)
      await start()
      await stop()
      expect(AiAudioController.audioToText).toHaveBeenCalledExactlyOnceWith({
        body: {
          uploadAudioFile: expect.any(File),
          midSpeech: false,
          titleIntent: true,
        },
      })
      expect(noteTitleText(wrapper)).toBe(savedTitle)
      await vi.runOnlyPendingTimersAsync()
      await flushPromises()
      expect(mockedUpdateTitleCall).toHaveBeenCalledWith(
        expect.objectContaining({
          body: expect.objectContaining({ newTitle: savedTitle }),
        })
      )
    }
  )

  it("replaces only the selected title text", async () => {
    placeCaretInTitle(wrapper, 0, 7)
    hearing("Garden")
    await start()
    expectTitleMarkerAfter(wrapper, 7)
    await stop()
    expect(noteTitleText(wrapper)).toBe("Garden notes")
    expect(titleCaretOffset()).toBe("Garden".length)
  })

  it("replaces the whole selected title", async () => {
    selectWholeTitle(wrapper)
    hearing("Pear orchard")
    await start()
    await stop()
    expect(noteTitleText(wrapper)).toBe("Pear orchard")
  })

  it.each(["initially unfocused", "after title dictation"])(
    "chooses content for a fresh recording %s",
    async (journey) => {
      if (journey === "after title dictation") {
        placeCaretInTitle(wrapper, 7)
        await start()
        await stop()
        const content = wrapper.find('.note-content [contenteditable="true"]')
          .element as HTMLElement
        content.focus()
      }
      await start()
      expect(titleEditable(wrapper)).toBe("true")
      await stop()
      expect(wrapper.find(".note-content").text()).toContain("harvest")
    }
  )

  it("keeps the title locked and its marker until conversion finishes", async () => {
    let finish!: () => void
    mockSdkService(
      AiAudioController,
      "audioToText",
      audioTextResponse("harvest")
    ).mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          finish = () => resolve(wrapSdkResponse(audioTextResponse("harvest")))
        })
    )
    placeCaretInTitle(wrapper, 7)
    await start()
    await stop()
    const button = wrapper.find('button[aria-label="Voice input"]')
    expect(button.attributes()).toHaveProperty("disabled")
    expect(button.find(".animate-spin").exists()).toBe(true)
    expect(titleEditable(wrapper)).toBe("false")
    expect(dictationMarker()).not.toBeNull()
    finish()
    await flushPromises()
    expect(noteTitleText(wrapper)).toBe("Orchard harvest notes")
    expect(button.attributes()).not.toHaveProperty("disabled")
    expect(button.find(".animate-spin").exists()).toBe(false)
  })

  it("captures the caret before delayed microphone startup and moved focus", async () => {
    const createRecorder = vi.mocked(createAudioRecorder)
    const implementation = createRecorder.getMockImplementation()!
    let finishStartup!: () => void
    createRecorder.mockImplementationOnce((callback, options) => {
      const recorder = implementation(callback, options!)
      vi.mocked(recorder.startRecording).mockImplementation(
        () =>
          new Promise<void>((resolve) => {
            finishStartup = resolve
          })
      )
      return recorder
    })
    placeCaretInTitle(wrapper, 7)
    await start()
    const button = wrapper.find('button[aria-label="Voice input"]')
    expect(button.attributes()).toHaveProperty("disabled")
    expect(button.find(".animate-spin").exists()).toBe(true)
    const otherButton = wrapper.find(
      'button[aria-label="Edit as markdown (m)"]'
    ).element as HTMLElement
    otherButton.focus()
    finishStartup()
    await flushPromises()
    await stop()
    expect(noteTitleText(wrapper)).toBe("Orchard harvest notes")
  })
})
