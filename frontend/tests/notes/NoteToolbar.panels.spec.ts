import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import makeMe from "donut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import {
  allMoreOptionsFitNavWidth,
  installMockResizeObserver,
  layoutNoteToolbar,
  overflowTogglesNavWidth,
  restoreNoteToolbarWidthMocks,
} from "@tests/helpers/mockNoteToolbarNavWidth"
import NoteMoreOptionsForm from "@/components/notes/widgets/NoteMoreOptionsForm.vue"
import { noteMoreOptionsTitles } from "@/components/notes/widgets/noteMoreOptionsTitles"
import {
  mountNoteToolbar,
  openNoteToolbarOverflowMenu,
  overflowMenuItem,
  resetNoteToolbarTestState,
} from "@tests/notes/noteToolbarTestHelpers"
import { useAssimilationView } from "@/composables/useAssimilationView"
import { useNoteToolbarPanel } from "@/composables/useNoteToolbarPanel"
import routes from "@/routes/routes"
import {
  notePropertyLocation,
  noteShowLocation,
} from "@/routes/noteShowLocation"
import {
  createRouter,
  createWebHistory,
  type RouteLocationNamedRaw,
  type Router,
} from "vue-router"
import { describe, it, expect, afterEach, beforeEach, vi } from "vitest"
import { type VueWrapper, flushPromises } from "@vue/test-utils"

const titles = noteMoreOptionsTitles

describe("NoteToolbar panels", () => {
  // biome-ignore lint/suspicious/noExplicitAny: wrapper for testing
  let wrapper: VueWrapper<any>
  const noteRealm = makeMe.aNoteRealm.please()
  const panelShell = () =>
    wrapper.find('[data-testid="note-toolbar-panel-shell"]')
  const assimilationModes = () =>
    wrapper.find('[data-testid="note-assimilation-modes"]')

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

  it("toggles the audio tools panel from the inline button and overflow menu", async () => {
    wrapper = await mountNoteToolbar(noteRealm)
    await layoutNoteToolbar(wrapper, allMoreOptionsFitNavWidth())

    const audioToolsButton = wrapper.find(`button[title="${titles.audio}"]`)
    const expectAudioButtonPressed = (pressed: boolean) => {
      expect(audioToolsButton.classes().includes("daisy-btn-soft")).toBe(
        pressed
      )
      expect(audioToolsButton.classes().includes("daisy-btn-primary")).toBe(
        pressed
      )
    }
    expectAudioButtonPressed(false)
    expect(panelShell().exists()).toBe(false)

    await audioToolsButton.trigger("click")
    await flushPromises()

    expect(panelShell().exists()).toBe(true)
    expect(useNoteToolbarPanel().isAudioOpen.value).toBe(true)
    expectAudioButtonPressed(true)
    expect(audioToolsButton.attributes("aria-pressed")).toBe("true")

    await audioToolsButton.trigger("click")
    await flushPromises()

    expect(panelShell().exists()).toBe(false)
    expectAudioButtonPressed(false)
    expect(audioToolsButton.attributes("aria-pressed")).toBe("false")

    await layoutNoteToolbar(wrapper, overflowTogglesNavWidth())
    await openNoteToolbarOverflowMenu(wrapper)
    overflowMenuItem(titles.audio)!.click()
    await flushPromises()

    expect(panelShell().exists()).toBe(true)
    expect(useNoteToolbarPanel().isAudioOpen.value).toBe(true)
    expect(document.querySelector("[data-dropdown-portal-panel]")).toBeNull()
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

  it("hides assimilation when audio opens and vice versa", async () => {
    wrapper = await mountNoteToolbar(noteRealm)
    useAssimilationView().openForNote(noteRealm.note.id)
    await flushPromises()

    await wrapper.find(`button[title="${titles.audio}"]`).trigger("click")
    await flushPromises()

    expect(assimilationModes().exists()).toBe(false)
    expect(wrapper.find('button[title="Record Audio"]').exists()).toBe(true)

    useAssimilationView().openForNote(noteRealm.note.id)
    await flushPromises()

    expect(assimilationModes().exists()).toBe(true)
    expect(wrapper.find('button[title="Record Audio"]').exists()).toBe(false)
  })

  describe("conversation", () => {
    async function routerAt(location: RouteLocationNamedRaw) {
      const router = createRouter({ history: createWebHistory(), routes })
      await router.push(location)
      return router
    }

    const startConversation = async () => {
      await wrapper.find(`[title="${titles.conversation}"]`).trigger("click")
      await flushPromises()
    }

    it("replaces conversation query on the current note location", async () => {
      const router = await routerAt(noteShowLocation(noteRealm.note.id))
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
        const router = await routerAt(location)
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
