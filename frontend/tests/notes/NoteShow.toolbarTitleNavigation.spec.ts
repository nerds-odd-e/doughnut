import {
  NoteController,
  TextContentController,
} from "@generated/donut-backend-api/sdk.gen"
import NoteShow from "@/components/notes/NoteShow.vue"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import helper, {
  mockSdkService,
  mockSdkServiceWithImplementation,
} from "@tests/helpers"
import { noteTitleText } from "@tests/notes/noteNewFormTestSupport"
import {
  placeCaretInTitle,
  microphoneCannotStart,
  useSpokenTitleTestLifecycle,
} from "@tests/notes/spokenTitleTestSupport"
import {
  editTitle,
  titleEditorEl,
  mockUpdateNoteTitle,
  mockedUpdateTitleCall,
} from "@tests/notes/noteTextContentTestSupport"
import {
  startVoiceInputByPointer,
  stopVoiceInputWithFocus,
} from "@tests/notes/noteVoiceInputButtonTestSupport"
import {
  flushReferencedTitleBlurDiscardCheck,
  referencedTitleSaveKeepVisibleTextSelector,
} from "@tests/notes/textContentWrapperTestSupport"
import {
  installMockResizeObserver,
  layoutNoteToolbar,
  overflowTogglesNavWidth,
  restoreNoteToolbarWidthMocks,
} from "@tests/helpers/mockNoteToolbarNavWidth"
import { overflowMenuItem } from "@tests/notes/noteToolbarTestHelpers"
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
useSpokenTitleTestLifecycle("Autumn")

describe("Reaching toolbar voice input from a title", () => {
  let wrapper: VueWrapper
  beforeEach(async () => {
    installMockResizeObserver()
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
    placeCaretInTitle(wrapper, 0, 6)
  })
  afterEach(() => {
    wrapper.unmount()
    restoreNoteToolbarWidthMocks()
    vi.unstubAllGlobals()
  })

  const focusToolbar = () => {
    const first = wrapper.find("[data-note-toolbar] button")
      .element as HTMLElement
    titleEditorEl(wrapper).dispatchEvent(
      new KeyboardEvent("keydown", { key: "Tab", bubbles: true })
    )
    first.focus()
  }
  const activate = async (button: HTMLElement, key: string) => {
    button.focus()
    button.dispatchEvent(new KeyboardEvent("keydown", { key, bubbles: true }))
    // Synthetic keyboard events do not perform the browser's button activation.
    button.dispatchEvent(new KeyboardEvent("keyup", { key, bubbles: true }))
    button.click()
    await flushPromises()
  }
  const openOverflow = async () => {
    const trigger = wrapper.find('[title="more options"]')
      .element as HTMLElement
    trigger.focus()
    trigger.click()
    await flushPromises()
  }

  it.each(["Enter", " "])(
    "keeps the selected draft through toolbar keyboard activation with %s",
    async (key) => {
      focusToolbar()
      await flushReferencedTitleBlurDiscardCheck()
      await activate(
        wrapper.find('button[aria-label="Voice input"]').element as HTMLElement,
        key
      )
      await stopVoiceInputWithFocus(wrapper)
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
    }
  )

  it("preserves selection through closing and reopening the portal, then pins Stop", async () => {
    await layoutNoteToolbar(wrapper, overflowTogglesNavWidth())
    await openOverflow()
    const trigger = wrapper.find('[title="more options"]')
      .element as HTMLElement
    trigger.click()
    await flushPromises()
    await openOverflow()
    await flushReferencedTitleBlurDiscardCheck()
    const action = overflowMenuItem("Voice input")!
    action.focus()
    action.click()
    await flushPromises()
    expect(
      wrapper.find('button[aria-label="Stop voice input"]').isVisible()
    ).toBe(true)
    await stopVoiceInputWithFocus(wrapper)
    expect(noteTitleText(wrapper)).toBe("Autumn orchard notes")
  })

  it("retains the menu draft after failed startup and allows a fresh recording", async () => {
    await layoutNoteToolbar(wrapper, overflowTogglesNavWidth())
    microphoneCannotStart()
    await openOverflow()
    await activate(overflowMenuItem("Voice input")!, "Enter")
    await flushReferencedTitleBlurDiscardCheck()
    expect(noteTitleText(wrapper)).toBe("Spring orchard notes")
    await openOverflow()
    await activate(overflowMenuItem("Voice input")!, " ")
    await stopVoiceInputWithFocus(wrapper)
    expect(noteTitleText(wrapper)).toBe("Autumn orchard notes")
  })

  it.each(["content", "outside", "body"])(
    "clears title intent after leaving the toolbar for %s",
    async (destination) => {
      if (destination !== "body") focusToolbar()
      if (destination === "content") {
        ;(
          wrapper.find('.note-content [contenteditable="true"]')
            .element as HTMLElement
        ).focus()
      } else if (destination === "body") {
        titleEditorEl(wrapper).blur()
      } else {
        const outside = document.createElement("button")
        document.body.append(outside)
        outside.focus()
        outside.remove()
      }
      await startVoiceInputByPointer(wrapper)
      await stopVoiceInputWithFocus(wrapper)
      expect(wrapper.find(".note-content").text()).toContain("Autumn")
    }
  )

  it("chooses content on another note after a toolbar title journey", async () => {
    focusToolbar()
    const next = makeMe.aNoteRealm.title("Pear notes").please()
    mockSdkService(NoteController, "showNote", next)
    await wrapper.setProps({ noteId: next.id })
    await flushPromises()
    await startVoiceInputByPointer(wrapper)
    await stopVoiceInputWithFocus(wrapper)
    expect(wrapper.find(".note-content").text()).toContain("Autumn")
  })
})
