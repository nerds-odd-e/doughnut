import NoteShowPageWithNotebookSidebarLayout from "@tests/fixtures/NoteShowPageWithNotebookSidebarLayout.vue"
import { screen, waitFor } from "@testing-library/vue"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, {
  mockNotebookGetForNoteRealm,
  mockShowNoteAccessory,
  mockSdkService,
  mockSdkServiceWithImplementation,
  wrapSdkError,
} from "@tests/helpers"
import { resetNotebookSidebarState } from "@/composables/useCurrentNoteSidebarState"
import { flushPromises } from "@vue/test-utils"
import { createRouter, createWebHistory } from "vue-router"
import routes from "@/routes/routes"
import { describe, it, beforeEach, expect, vi } from "vitest"
import { NoteController } from "@generated/doughnut-backend-api/sdk.gen"
import type {
  NoteRealm,
  Options,
  ShowNoteData,
} from "@generated/doughnut-backend-api"

describe("all in note show page", () => {
  let router: ReturnType<typeof createRouter>

  beforeEach(() => {
    resetNotebookSidebarState()
    router = createRouter({
      history: createWebHistory(),
      routes,
    })
    mockShowNoteAccessory()
  })

  describe("note show by ambiguous slug", () => {
    const noteRealm = makeMe.aNoteRealm.please()

    beforeEach(() => {
      mockSdkService("showNoteByAmbiguousBasename", noteRealm)
      mockSdkService("showNote", noteRealm)
      mockNotebookGetForNoteRealm(noteRealm, {
        id: 101,
        name: "a circle",
      })
    })

    it("resolves ambiguous slug then loads note and passes stable id to NoteShow via storage", async () => {
      const basenameSpy = mockSdkService(
        "showNoteByAmbiguousBasename",
        noteRealm
      )
      const showNoteSpy = mockSdkService("showNote", noteRealm)

      helper
        .component(NoteShowPageWithNotebookSidebarLayout)
        .withCleanStorage()
        .withProps({ slug: "my-note" })
        .withRouter(router)
        .render()

      await flushPromises()
      await screen.findByText(noteRealm.note.noteTopology.title!)

      expect(basenameSpy).toHaveBeenCalledWith({
        path: { basename: "my-note" },
      })
      expect(showNoteSpy).toHaveBeenCalledWith({
        path: { note: noteRealm.id },
      })
    })

    it("shows error when ambiguous slug lookup fails", async () => {
      vi.spyOn(NoteController, "showNoteByAmbiguousBasename").mockResolvedValue(
        wrapSdkError({
          message: "More than one note matches this name.",
        }) as never
      )

      helper
        .component(NoteShowPageWithNotebookSidebarLayout)
        .withCleanStorage()
        .withProps({ slug: "ambiguous" })
        .withRouter(router)
        .render()

      expect(
        await screen.findByText(/More than one note matches this name/)
      ).toBeTruthy()
    })
  })

  describe("note show by notebook slug path", () => {
    const noteRealm = makeMe.aNoteRealm.please()

    beforeEach(() => {
      mockSdkService("getNoteBySlug", noteRealm)
      mockSdkService("showNote", noteRealm)
      mockNotebookGetForNoteRealm(noteRealm, {
        id: 101,
        name: "a circle",
      })
    })

    it("resolves slug path then loads note and passes stable id to NoteShow via storage", async () => {
      const slugSpy = mockSdkService("getNoteBySlug", noteRealm)
      const showNoteSpy = mockSdkService("showNote", noteRealm)

      helper
        .component(NoteShowPageWithNotebookSidebarLayout)
        .withCleanStorage()
        .withProps({
          notebookId: 99,
          noteSlugPath: "outer/inner/leaf",
        })
        .withRouter(router)
        .render()

      await flushPromises()
      await screen.findByText(noteRealm.note.noteTopology.title!)

      expect(slugSpy).toHaveBeenCalledWith({
        path: { notebook: 99 },
        query: { slugPath: "outer/inner/leaf" },
      })
      expect(showNoteSpy).toHaveBeenCalledWith({
        path: { note: noteRealm.id },
      })
    })
  })

  describe("stable sidebar tree while same-notebook slug resolves", () => {
    it("keeps root tree visible before delayed getNoteBySlug finishes and does not refetch root notes", async () => {
      const realmA = makeMe.aNoteRealm.title("Alpha").please()
      const realmB = makeMe.aNoteRealm.title("Beta").please()
      const sharedNotebookId = 99
      for (const r of [realmA, realmB]) {
        r.notebookId = sharedNotebookId
        r.note.noteTopology.notebookId = sharedNotebookId
      }
      const shallowA = {
        ...realmA,
        relationshipsDeprecating: undefined,
      } as NoteRealm
      const shallowB = {
        ...realmB,
        relationshipsDeprecating: undefined,
      } as NoteRealm

      const realmById: Record<number, NoteRealm> = {
        [realmA.id]: realmA,
        [realmB.id]: realmB,
      }
      mockSdkServiceWithImplementation("showNote", (options) => {
        const id = (options as Options<ShowNoteData>).path.note
        const r = realmById[id]
        if (r === undefined) {
          throw new Error(`NoteShowPage.spec: unmocked showNote ${id}`)
        }
        return r
      })

      let releaseSecondSlug: (() => void) | undefined
      mockSdkServiceWithImplementation("getNoteBySlug", async (options) => {
        const slugPath = String(
          (options as { query: { slugPath: string } }).query.slugPath
        )
        if (slugPath === realmB.slug) {
          await new Promise<void>((resolve) => {
            releaseSecondSlug = resolve
          })
          return realmB
        }
        return realmA
      })

      const rootListSpy = mockSdkService("listNotebookRootNotes", {
        notes: [shallowA, shallowB],
        folders: [],
      })

      mockNotebookGetForNoteRealm(realmA, {
        id: 101,
        name: "a circle",
      })

      const wrapper = helper
        .component(NoteShowPageWithNotebookSidebarLayout)
        .withCurrentUser(makeMe.aUser.please())
        .withCleanStorage()
        .withProps({
          notebookId: sharedNotebookId,
          noteSlugPath: realmA.slug,
        })
        .withRouter(router)
        .mount({ attachTo: document.body })

      await flushPromises()
      await vi.waitUntil(() => {
        const el = document.querySelector('[data-test="note-title"]')
        return el?.textContent?.trim() === "Alpha"
      })
      expect(rootListSpy).toHaveBeenCalledTimes(1)

      await wrapper.setProps({
        notebookId: sharedNotebookId,
        noteSlugPath: realmB.slug,
      })
      await flushPromises()

      expect(screen.getAllByText("Alpha").length).toBeGreaterThan(0)
      expect(rootListSpy).toHaveBeenCalledTimes(1)
      expect(releaseSecondSlug).toBeDefined()

      releaseSecondSlug!()
      await flushPromises()
      await waitFor(() => {
        const el = document.querySelector('[data-test="note-title"]')
        expect(el?.textContent?.trim()).toBe("Beta")
      })
    })

    it("keeps root tree visible before delayed showNoteByAmbiguousBasename finishes and does not refetch root notes", async () => {
      const realmA = makeMe.aNoteRealm.title("Alpha").please()
      const realmB = makeMe.aNoteRealm.title("Beta").please()
      const sharedNotebookId = 99
      for (const r of [realmA, realmB]) {
        r.notebookId = sharedNotebookId
        r.note.noteTopology.notebookId = sharedNotebookId
      }
      const shallowA = {
        ...realmA,
        relationshipsDeprecating: undefined,
      } as NoteRealm
      const shallowB = {
        ...realmB,
        relationshipsDeprecating: undefined,
      } as NoteRealm

      const realmById: Record<number, NoteRealm> = {
        [realmA.id]: realmA,
        [realmB.id]: realmB,
      }
      mockSdkServiceWithImplementation("showNote", (options) => {
        const id = (options as Options<ShowNoteData>).path.note
        const r = realmById[id]
        if (r === undefined) {
          throw new Error(`NoteShowPage.spec: unmocked showNote ${id}`)
        }
        return r
      })

      let releaseSecondBasename: (() => void) | undefined
      mockSdkServiceWithImplementation(
        "showNoteByAmbiguousBasename",
        async (options) => {
          const basename = (options as { path: { basename: string } }).path
            .basename
          if (basename === "slug-b") {
            await new Promise<void>((resolve) => {
              releaseSecondBasename = resolve
            })
            return realmB
          }
          return realmA
        }
      )

      const rootListSpy = mockSdkService("listNotebookRootNotes", {
        notes: [shallowA, shallowB],
        folders: [],
      })

      mockNotebookGetForNoteRealm(realmA, {
        id: 101,
        name: "a circle",
      })

      const wrapper = helper
        .component(NoteShowPageWithNotebookSidebarLayout)
        .withCurrentUser(makeMe.aUser.please())
        .withCleanStorage()
        .withProps({
          slug: "slug-a",
        })
        .withRouter(router)
        .mount({ attachTo: document.body })

      await flushPromises()
      await vi.waitUntil(() => {
        const el = document.querySelector('[data-test="note-title"]')
        return el?.textContent?.trim() === "Alpha"
      })
      const rootCallsAfterAlpha = rootListSpy.mock.calls.length
      expect(rootCallsAfterAlpha).toBeGreaterThan(0)

      await wrapper.setProps({
        slug: "slug-b",
      })
      await flushPromises()

      expect(screen.getAllByText("Alpha").length).toBeGreaterThan(0)
      expect(rootListSpy.mock.calls.length).toBe(rootCallsAfterAlpha)
      expect(releaseSecondBasename).toBeDefined()

      releaseSecondBasename!()
      await flushPromises()
      expect(rootListSpy.mock.calls.length).toBe(rootCallsAfterAlpha)
      await waitFor(() => {
        const el = document.querySelector('[data-test="note-title"]')
        expect(el?.textContent?.trim()).toBe("Beta")
      })
    })
  })

  describe("conversation maximize/minimize", () => {
    const note = makeMe.aNoteRealm.please()

    beforeEach(() => {
      mockSdkService("getNoteBySlug", note)
      mockSdkService("showNote", note)
      mockSdkService("getConversationsAboutNote", [])
      mockNotebookGetForNoteRealm(note)
    })

    it("should maximize conversation when maximize button is clicked", async () => {
      const wrapper = helper
        .component(NoteShowPageWithNotebookSidebarLayout)
        .withCurrentUser(makeMe.aUser.please())
        .withCleanStorage()
        .withProps({
          notebookId: note.notebookId,
          noteSlugPath: note.slug,
        })
        .withRouter(router)
        .mount()

      await flushPromises()

      await router.push({
        name: "noteShow",
        params: {
          notebookId: String(note.notebookId),
          noteSlugPath: note.slug,
        },
        query: { conversation: "true" },
      })
      await flushPromises()

      await wrapper.find('[aria-label="Toggle maximize"]').trigger("click")
      expect(wrapper.find(".note-content-wrapper").exists()).toBe(false)

      await wrapper.find('[aria-label="Toggle maximize"]').trigger("click")
      expect(wrapper.find(".note-content-wrapper").exists()).toBe(true)
    })

    it("should restore maximized state before closing conversation", async () => {
      const wrapper = helper
        .component(NoteShowPageWithNotebookSidebarLayout)
        .withCurrentUser(makeMe.aUser.please())
        .withCleanStorage()
        .withProps({
          notebookId: note.notebookId,
          noteSlugPath: note.slug,
        })
        .withRouter(router)
        .mount()

      await flushPromises()

      await router.push({
        name: "noteShow",
        params: {
          notebookId: String(note.notebookId),
          noteSlugPath: note.slug,
        },
        query: { conversation: "true" },
      })
      await flushPromises()

      await wrapper.find('[aria-label="Toggle maximize"]').trigger("click")
      expect(wrapper.find(".note-content-wrapper").exists()).toBe(false)

      await wrapper.find('[aria-label="Close dialog"]').trigger("click")
      await flushPromises()

      expect(router.currentRoute.value.name).toBe("noteShow")
      expect(router.currentRoute.value.params.notebookId).toBe(
        String(note.notebookId)
      )
      expect(router.currentRoute.value.params.noteSlugPath).toBe(note.slug)
      expect(router.currentRoute.value.query.conversation).toBeUndefined()
      expect(wrapper.find(".note-content-wrapper").exists()).toBe(true)
      expect(wrapper.find(".conversation-container").exists()).toBe(false)
    })

    it("should open conversation when URL has conversation=true", async () => {
      router.push({
        name: "noteShow",
        params: {
          notebookId: String(note.notebookId),
          noteSlugPath: note.slug,
        },
        query: { conversation: "true" },
      })
      await flushPromises()

      const wrapper = helper
        .component(NoteShowPageWithNotebookSidebarLayout)
        .withCurrentUser(makeMe.aUser.please())
        .withCleanStorage()
        .withProps({
          notebookId: note.notebookId,
          noteSlugPath: note.slug,
        })
        .withRouter(router)
        .mount()

      await flushPromises()

      expect(wrapper.find(".conversation-wrapper").exists()).toBe(true)
    })
  })
})
