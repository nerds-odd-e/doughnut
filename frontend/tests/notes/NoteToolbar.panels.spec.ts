import {
  AiAudioController,
  NoteController,
  TextContentController,
} from "@generated/donut-backend-api/sdk.gen"
import makeMe from "donut-test-fixtures/makeMe"
import helper, { mockSdkService, productionRouterAt } from "@tests/helpers"
import {
  allMoreOptionsFitNavWidth,
  installMockResizeObserver,
  layoutNoteToolbar,
  restoreNoteToolbarWidthMocks,
} from "@tests/helpers/mockNoteToolbarNavWidth"
import NoteMoreOptionsForm from "@/components/notes/widgets/NoteMoreOptionsForm.vue"
import {
  noteMoreOptionsTitles,
  noteVoiceInputTitles,
} from "@/components/notes/widgets/noteMoreOptionsTitles"
import {
  mountNoteToolbar,
  noteToolbarAction,
  noteToolbarProps,
  resetNoteToolbarTestState,
} from "@tests/notes/noteToolbarTestHelpers"
import { useAssimilationView } from "@/composables/useAssimilationView"
import { useNoteStore } from "@/store/noteStore"
import {
  notePropertyLocation,
  noteShowLocation,
} from "@/routes/noteShowLocation"
import type { Router } from "vue-router"
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

describe("NoteToolbar panels", () => {
  // biome-ignore lint/suspicious/noExplicitAny: wrapper for testing
  let wrapper: VueWrapper<any>
  const noteRealm = makeMe.aNoteRealm.please()
  const panelShell = () =>
    wrapper.find('[data-testid="note-toolbar-panel-shell"]')

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

  it("copies export markdown while keeping the export dialog open", async () => {
    mockSdkService(NoteController, "getAiContextMarkdown", {
      markdown: "# AI context\n\nHello **world**.",
    })
    const writeText = vi.fn().mockResolvedValue(undefined)
    vi.stubGlobal("navigator", { ...navigator, clipboard: { writeText } })

    wrapper = await mountNoteToolbar(noteRealm)
    await layoutNoteToolbar(wrapper, allMoreOptionsFitNavWidth())
    await wrapper.find(`button[title="${titles.export}"]`).trigger("click")

    const dialog = document.querySelector("dialog") as HTMLDialogElement
    expect(dialog?.open).toBe(true)

    document
      .querySelector<HTMLButtonElement>(
        '[data-testid="copy-ai-context-md-btn"]'
      )!
      .click()
    await flushPromises()

    expect(writeText).toHaveBeenCalledWith(
      expect.stringContaining("AI context")
    )
    expect(dialog.open).toBe(true)
  })

  it("starts and stops voice input from the inline button", async () => {
    wrapper = await mountNoteToolbar(noteRealm)
    await layoutNoteToolbar(wrapper, allMoreOptionsFitNavWidth())

    await noteToolbarAction(wrapper, noteVoiceInputTitles.start).trigger(
      "click"
    )
    await flushPromises()

    const stopButton = noteToolbarAction(wrapper, noteVoiceInputTitles.stop)
    expect(stopButton.attributes("aria-pressed")).toBe("true")
    expect(stopButton.classes()).toEqual(
      expect.arrayContaining(["daisy-btn-soft", "daisy-btn-primary"])
    )
    expect(panelShell().exists()).toBe(false)

    await stopButton.trigger("click")
    await flushPromises()

    const idleButton = noteToolbarAction(wrapper, noteVoiceInputTitles.start)
    expect(idleButton.attributes()).not.toHaveProperty("aria-pressed")
    expect(idleButton.classes()).not.toContain("daisy-btn-primary")
  })

  it("dictates into the note on the page when voice input starts after moving to another note", async () => {
    const saveContent = mockSdkService(
      TextContentController,
      "updateNoteContent",
      makeMe.aNoteRealm.please()
    )
    mockSdkService(AiAudioController, "audioToText", {
      segmentTexts: ["hello"],
      endTimestamp: "00:00:01,000",
    })
    const destination = makeMe.aNoteRealm.content("Destination.").please()
    wrapper = await mountNoteToolbar(noteRealm)
    useNoteStore().refreshNoteRealm(destination)
    await wrapper.setProps(noteToolbarProps(destination))
    await flushPromises()

    await noteToolbarAction(wrapper, noteVoiceInputTitles.start).trigger(
      "click"
    )
    await flushPromises()
    const { createAudioRecorder } = await import("@/models/audio/audioRecorder")
    const convert = vi.mocked(createAudioRecorder).mock.lastCall![0]
    await convert({ data: new File([], "test.webm"), isMidSpeech: false })

    expect(saveContent).toHaveBeenCalledExactlyOnceWith({
      path: { note: destination.note.id },
      body: { content: "Destination. hello" },
    })
  })

  it("shows assimilation settings in the shared panel shell without a max-height cage", async () => {
    wrapper = await mountNoteToolbar(noteRealm)
    useAssimilationView().openForNote(noteRealm.note.id)
    await flushPromises()

    const modes = panelShell().find('[data-testid="note-assimilation-modes"]')
    expect(modes.exists()).toBe(true)
    expect(modes.find(".max-h-\\[min\\(40vh\\,22rem\\)\\]").exists()).toBe(
      false
    )

    useAssimilationView().dismiss()
    await flushPromises()

    expect(panelShell().exists()).toBe(false)
  })

  describe("conversation", () => {
    const startConversation = async () => {
      await wrapper.find(`[title="${titles.conversation}"]`).trigger("click")
      await flushPromises()
    }

    it("replaces conversation query on the current note location", async () => {
      const router = await productionRouterAt(
        noteShowLocation(noteRealm.note.id)
      )
      wrapper = await mountNoteToolbar(noteRealm, { router })
      const replaceSpy = vi.spyOn(router, "replace")
      const pushSpy = vi.spyOn(router, "push")

      await startConversation()

      expect(pushSpy).not.toHaveBeenCalled()
      expect(replaceSpy).toHaveBeenCalledTimes(1)
      expect(router.currentRoute.value).toMatchObject({
        ...noteShowLocation(noteRealm.note.id),
        query: { conversation: "true" },
      })
    })

    it.each([
      {
        from: "the toolbar",
        mount: (router: Router) => mountNoteToolbar(noteRealm, { router }),
      },
      {
        from: "the overflow menu",
        mount: (router: Router) =>
          helper
            .component(NoteMoreOptionsForm)
            .withRouter(router)
            .withProps({ note: noteRealm.note, only: ["conversation"] })
            .mount(),
      },
    ])(
      "keeps the focused property when starting a conversation from $from",
      async ({ mount }) => {
        const location = notePropertyLocation(noteRealm.note.id, "topic")
        const router = await productionRouterAt(location)
        wrapper = await mount(router)

        await startConversation()

        expect(router.currentRoute.value).toMatchObject(location)
        expect(router.currentRoute.value.query).toEqual({
          conversation: "true",
        })
      }
    )
  })
})
