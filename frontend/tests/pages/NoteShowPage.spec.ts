import NoteShowPageWithNotebookSidebarLayout from "@tests/fixtures/NoteShowPageWithNotebookSidebarLayout.vue"
import { screen, waitFor } from "@testing-library/vue"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, {
  mockNotebookGetForNoteRealm,
  mockShowNoteAccessory,
  mockSdkService,
  mockSdkServiceWithImplementation,
} from "@tests/helpers"
import { resetNotebookSidebarState } from "@/composables/useCurrentNoteSidebarState"
import { flushPromises } from "@vue/test-utils"
import { createRouter, createWebHistory } from "vue-router"
import routes from "@/routes/routes"
import { describe, it, beforeEach, expect, vi } from "vitest"
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

  describe("note show by note id", () => {
    const noteRealm = makeMe.aNoteRealm.please()

    beforeEach(() => {
      mockSdkService("showNote", noteRealm)
      mockNotebookGetForNoteRealm(noteRealm, {
        id: 101,
        name: "a circle",
      })
    })

    it("loads note by id from route and does not resolve by notebook slug", async () => {
      const showNoteSpy = mockSdkService("showNote", noteRealm)
      const slugSpy = mockSdkService("getNoteBySlug", noteRealm)

      helper
        .component(NoteShowPageWithNotebookSidebarLayout)
        .withCleanStorage()
        .withProps({ noteId: noteRealm.id })
        .withRouter(router)
        .render()

      await flushPromises()
      await screen.findByText(noteRealm.note.noteTopology.title!)

      expect(showNoteSpy).toHaveBeenCalledWith({
        path: { note: noteRealm.id },
      })
      expect(slugSpy).not.toHaveBeenCalled()
    })
  })

  describe("note show by notebook slug path (legacy wiki URLs)", () => {
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
  })

  describe("conversation maximize/minimize", () => {
    const note = makeMe.aNoteRealm.please()

    beforeEach(() => {
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
          noteId: note.id,
        })
        .withRouter(router)
        .mount()

      await flushPromises()

      await router.push({
        name: "noteShow",
        params: {
          noteId: String(note.id),
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
          noteId: note.id,
        })
        .withRouter(router)
        .mount()

      await flushPromises()

      await router.push({
        name: "noteShow",
        params: {
          noteId: String(note.id),
        },
        query: { conversation: "true" },
      })
      await flushPromises()

      await wrapper.find('[aria-label="Toggle maximize"]').trigger("click")
      expect(wrapper.find(".note-content-wrapper").exists()).toBe(false)

      await wrapper.find('[aria-label="Close dialog"]').trigger("click")
      await flushPromises()

      expect(router.currentRoute.value.name).toBe("noteShow")
      expect(router.currentRoute.value.params.noteId).toBe(String(note.id))
      expect(router.currentRoute.value.query.conversation).toBeUndefined()
      expect(wrapper.find(".note-content-wrapper").exists()).toBe(true)
      expect(wrapper.find(".conversation-container").exists()).toBe(false)
    })

    it("should open conversation when URL has conversation=true", async () => {
      router.push({
        name: "noteShow",
        params: {
          noteId: String(note.id),
        },
        query: { conversation: "true" },
      })
      await flushPromises()

      const wrapper = helper
        .component(NoteShowPageWithNotebookSidebarLayout)
        .withCurrentUser(makeMe.aUser.please())
        .withCleanStorage()
        .withProps({
          noteId: note.id,
        })
        .withRouter(router)
        .mount()

      await flushPromises()

      expect(wrapper.find(".conversation-wrapper").exists()).toBe(true)
    })
  })
})
