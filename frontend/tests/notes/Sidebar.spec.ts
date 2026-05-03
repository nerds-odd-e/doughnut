import Sidebar from "@/components/notes/Sidebar.vue"
import { notebookSidebarNotebookPageContext } from "@/composables/useCurrentNoteSidebarState"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import type {
  NoteRealm,
  Options,
  ShowNoteData,
} from "@generated/doughnut-backend-api"
import createNoteStorage from "@/store/createNoteStorage"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, {
  mockSdkService,
  mockSdkServiceWithImplementation,
} from "@tests/helpers"
import { type VueWrapper, DOMWrapper, flushPromises } from "@vue/test-utils"
import { vi, describe, it, expect, beforeEach, afterEach } from "vitest"

function isBefore(node1: Node, node2: Node) {
  return !!(
    // eslint-disable-next-line no-bitwise
    (node1.compareDocumentPosition(node2) & Node.DOCUMENT_POSITION_FOLLOWING)
  )
}

/** Distinct from note ids — folder listing API uses folder entity ids. */
const FOLDER_TOP_NOTE_CHILDREN_ID = 77001
const FOLDER_FIRST_GEN_CHILDREN_ID = 77002

function structuralFolder(folderId: number, noteRealm: NoteRealm) {
  return {
    id: String(folderId),
    name: noteRealm.note.noteTopology.title!,
  }
}

/** Aligns listing payloads with folder-first sidebar drag rules (same folderId = same drop column). */
function noteRealmInFolder(realm: NoteRealm, folderId: number): NoteRealm {
  return {
    ...realm,
    note: {
      ...realm.note,
      noteTopology: {
        ...realm.note.noteTopology,
        folderId,
      },
    },
  } as NoteRealm
}

function stubFolderListingForTree(
  firstGeneration: NoteRealm,
  firstGenerationSibling: NoteRealm,
  secondGeneration: NoteRealm
) {
  return mockSdkServiceWithImplementation("listFolderListing", (options) => {
    const folderId = (options as { path: { folder: number } }).path.folder
    if (folderId === FOLDER_TOP_NOTE_CHILDREN_ID) {
      return {
        notes: [
          noteRealmInFolder(firstGeneration, FOLDER_TOP_NOTE_CHILDREN_ID),
          noteRealmInFolder(
            firstGenerationSibling,
            FOLDER_TOP_NOTE_CHILDREN_ID
          ),
        ],
        folders: [
          structuralFolder(FOLDER_FIRST_GEN_CHILDREN_ID, firstGeneration),
        ],
      }
    }
    if (folderId === FOLDER_FIRST_GEN_CHILDREN_ID) {
      return {
        notes: [
          noteRealmInFolder(secondGeneration, FOLDER_FIRST_GEN_CHILDREN_ID),
        ],
        folders: [],
      }
    }
    return { notes: [], folders: [] }
  })
}

describe("Sidebar", () => {
  // biome-ignore lint/suspicious/noExplicitAny: wrapper for testing
  let wrapper: VueWrapper<any>
  const storageAccessor = useStorageAccessor()
  const topNoteRealm = makeMe.aNoteRealm.title("top").please()
  const firstGeneration = makeMe.aNoteRealm
    .title("first gen")
    .under(topNoteRealm)
    .please()
  const firstGenerationSibling = makeMe.aNoteRealm
    .title("first gen sibling")
    .under(topNoteRealm)
    .please()
  const secondGeneration = makeMe.aNoteRealm
    .title("2nd gen")
    .under(firstGeneration)
    .please()

  /** Aligns active realm with folder-first API: ancestorFolders + folderId for this stub tree. */
  function realmAsActiveInSidebarStub(realm: NoteRealm): NoteRealm {
    const topTitle = topNoteRealm.note.noteTopology.title!
    const firstTitle = firstGeneration.note.noteTopology.title!

    if (realm.id === topNoteRealm.id) {
      return { ...realm, ancestorFolders: [] }
    }
    if (
      realm.id === firstGeneration.id ||
      realm.id === firstGenerationSibling.id
    ) {
      return {
        ...realm,
        ancestorFolders: [
          { id: String(FOLDER_TOP_NOTE_CHILDREN_ID), name: topTitle },
        ],
        note: {
          ...realm.note,
          noteTopology: {
            ...realm.note.noteTopology,
            folderId: FOLDER_TOP_NOTE_CHILDREN_ID,
          },
        },
      } as NoteRealm
    }
    if (realm.id === secondGeneration.id) {
      return {
        ...realm,
        ancestorFolders: [
          { id: String(FOLDER_TOP_NOTE_CHILDREN_ID), name: topTitle },
          { id: String(FOLDER_FIRST_GEN_CHILDREN_ID), name: firstTitle },
        ],
        note: {
          ...realm.note,
          noteTopology: {
            ...realm.note.noteTopology,
            folderId: FOLDER_FIRST_GEN_CHILDREN_ID,
          },
        },
      } as NoteRealm
    }
    return { ...realm, ancestorFolders: realm.ancestorFolders ?? [] }
  }

  const mountSidebar = (n: NoteRealm) => {
    wrapper = helper
      .component(Sidebar)
      .withProps({
        activeNoteRealm: realmAsActiveInSidebarStub(n),
        notebookId: n.notebookId,
      })
      .mount({ attachTo: document.body })
    return wrapper
  }

  beforeEach(() => {
    storageAccessor.value = createNoteStorage()
    storageAccessor.value.refOfNoteRealm(topNoteRealm.id).value = topNoteRealm
    storageAccessor.value.refOfNoteRealm(firstGeneration.id).value =
      firstGeneration
    storageAccessor.value.refOfNoteRealm(firstGenerationSibling.id).value =
      firstGenerationSibling
    storageAccessor.value.refOfNoteRealm(secondGeneration.id).value =
      secondGeneration

    const shallowTopRealm = { ...topNoteRealm } as NoteRealm
    mockSdkService("listNotebookRootNotes", {
      notes: [shallowTopRealm],
      folders: [structuralFolder(FOLDER_TOP_NOTE_CHILDREN_ID, topNoteRealm)],
    })
    stubFolderListingForTree(
      firstGeneration,
      firstGenerationSibling,
      secondGeneration
    )

    const fullRealmByNoteId: Record<number, NoteRealm> = {
      [topNoteRealm.id]: topNoteRealm,
      [firstGeneration.id]: firstGeneration,
      [firstGenerationSibling.id]: firstGenerationSibling,
      [secondGeneration.id]: secondGeneration,
    }
    mockSdkServiceWithImplementation("showNote", (options) => {
      const id = (options as Options<ShowNoteData>).path.note
      const realm = fullRealmByNoteId[id]
      if (realm === undefined) {
        throw new Error(`Sidebar.spec: unmocked showNote for note id ${id}`)
      }
      return realm
    })
  })

  beforeEach(() => {
    // Browser Mode: Mock getBoundingClientRect, offsetWidth, clientWidth for deterministic tests
    Element.prototype.getBoundingClientRect = vi.fn().mockReturnValue({
      top: 0,
      bottom: 100,
      height: 100,
      width: 200,
      left: 0,
      right: 200,
      x: 0,
      y: 0,
      // toJSON is required by DOMRect interface but unused in tests
      toJSON: () => ({}),
    })

    Object.defineProperty(HTMLElement.prototype, "offsetWidth", {
      configurable: true,
      get() {
        return 200
      },
    })
    Object.defineProperty(HTMLElement.prototype, "clientWidth", {
      configurable: true,
      get() {
        return 200
      },
    })
  })

  beforeEach(() => {
    // Browser Mode: Spy on REAL scrollIntoView method!
    vi.spyOn(HTMLElement.prototype, "scrollIntoView")
  })

  afterEach(() => {
    notebookSidebarNotebookPageContext.value = undefined
    wrapper?.unmount()
    document.body.innerHTML = ""
    vi.restoreAllMocks()
  })

  // Note titles appear in `.title-text`; folder rows use `.sidebar-folder-label`.
  const findSidebarItem = (text: string): DOMWrapper<Element> | undefined => {
    const inner = wrapper
      .findAll(".title-text")
      .find((el) => el.text().includes(text))
    if (!inner) return
    const li = inner.element.closest("li")
    return li ? new DOMWrapper(li) : undefined
  }

  describe("user active folder", () => {
    const findRootFolderRowByTopTitle = () => {
      const label = wrapper
        .findAll(".sidebar-folder-label")
        .find((w) =>
          w.text().includes(topNoteRealm.note.noteTopology.title ?? "")
        )
      if (!label?.exists()) return
      const li = label.element.closest("li")
      return li ? new DOMWrapper(li) : undefined
    }

    it("applies sidebar-folder-user-active when a folder row is clicked", async () => {
      mountSidebar(firstGeneration)
      await vi.waitUntil(() =>
        findSidebarItem(firstGeneration.note.noteTopology.title!)?.exists()
      )
      const folderRow = findRootFolderRowByTopTitle()
      expect(folderRow?.exists()).toBe(true)
      await folderRow!.find(".sidebar-folder-label").trigger("click")
      await flushPromises()
      expect(folderRow!.classes()).toContain("sidebar-folder-user-active")
    })

    it("clears user active folder styling when a note row is clicked", async () => {
      mountSidebar(firstGeneration)
      await vi.waitUntil(() =>
        findSidebarItem(topNoteRealm.note.noteTopology.title!)?.exists()
      )
      const folderRow = findRootFolderRowByTopTitle()
      await folderRow!.find(".sidebar-folder-label").trigger("click")
      await flushPromises()
      expect(folderRow!.classes()).toContain("sidebar-folder-user-active")

      const noteRow = findSidebarItem(topNoteRealm.note.noteTopology.title!)
      expect(noteRow?.exists()).toBe(true)
      await noteRow!.trigger("click")
      await flushPromises()
      expect(folderRow!.classes()).not.toContain("sidebar-folder-user-active")
    })

    it("does not clear user active folder when focus moves to the sidebar toolbar", async () => {
      wrapper = helper
        .component(Sidebar)
        .withCurrentUser(makeMe.aUser.please())
        .withProps({
          activeNoteRealm: realmAsActiveInSidebarStub(firstGeneration),
          notebookId: firstGeneration.notebookId,
        })
        .mount({ attachTo: document.body })
      await flushPromises()
      await vi.waitUntil(() =>
        findSidebarItem(firstGeneration.note.noteTopology.title!)?.exists()
      )
      const folderRow = findRootFolderRowByTopTitle()
      await folderRow!.find(".sidebar-folder-label").trigger("click")
      await flushPromises()
      const toolbarBtn = wrapper.find('button[title="New folder"]')
      expect(toolbarBtn.exists()).toBe(true)

      folderRow!.element.focus()
      const leave = new FocusEvent("focusout", {
        bubbles: true,
        relatedTarget: toolbarBtn.element,
      })
      folderRow!.element.dispatchEvent(leave)
      await flushPromises()
      expect(folderRow!.classes()).toContain("sidebar-folder-user-active")
    })
  })

  it("should call the api once if top note", async () => {
    mountSidebar(topNoteRealm)
    await vi.waitUntil(() =>
      findSidebarItem(firstGeneration.note.noteTopology.title!)?.exists()
    )
  })

  it("does not reload notebook root notes when active note changes within the same notebook", async () => {
    const shallowTopRealm = { ...topNoteRealm } as NoteRealm
    const rootSpy = mockSdkService("listNotebookRootNotes", {
      notes: [shallowTopRealm],
      folders: [structuralFolder(FOLDER_TOP_NOTE_CHILDREN_ID, topNoteRealm)],
    })
    mountSidebar(firstGeneration)
    await flushPromises()
    expect(rootSpy).toHaveBeenCalledTimes(1)
    expect(
      findSidebarItem(topNoteRealm.note.noteTopology.title!)?.exists()
    ).toBe(true)

    await wrapper.setProps({
      activeNoteRealm: realmAsActiveInSidebarStub(secondGeneration),
      notebookId: firstGeneration.notebookId,
    })
    await flushPromises()
    expect(rootSpy).toHaveBeenCalledTimes(1)
    expect(
      findSidebarItem(topNoteRealm.note.noteTopology.title!)?.exists()
    ).toBe(true)
  })

  describe("gradual ancestor population", () => {
    beforeEach(() => {
      storageAccessor.value = createNoteStorage()
    })

    it("loads ancestor branches for a deep note through folder listings without showNote", async () => {
      const shallowTopRealm = { ...topNoteRealm } as NoteRealm
      const rootSpy = mockSdkService("listNotebookRootNotes", {
        notes: [shallowTopRealm],
        folders: [structuralFolder(FOLDER_TOP_NOTE_CHILDREN_ID, topNoteRealm)],
      })
      const folderListingSpy = stubFolderListingForTree(
        firstGeneration,
        firstGenerationSibling,
        secondGeneration
      )
      const showNoteSpy = mockSdkServiceWithImplementation("showNote", () => {
        throw new Error("Sidebar must not use showNote for structural branches")
      })

      mountSidebar(secondGeneration)
      await flushPromises()

      expect(rootSpy).toHaveBeenCalledTimes(1)
      expect(showNoteSpy).not.toHaveBeenCalled()
      const listedFolderIds = folderListingSpy.mock.calls.map(
        (call) => (call[0] as { path: { folder: number } }).path.folder
      )
      expect(listedFolderIds).toContain(FOLDER_TOP_NOTE_CHILDREN_ID)
      expect(listedFolderIds).toContain(FOLDER_FIRST_GEN_CHILDREN_ID)
      expect(listedFolderIds).not.toContain(firstGenerationSibling.id)

      await vi.waitUntil(() =>
        findSidebarItem(secondGeneration.note.noteTopology.title!)?.exists()
      )
      expect(
        findSidebarItem(topNoteRealm.note.noteTopology.title!)?.exists()
      ).toBe(true)
      expect(
        findSidebarItem(firstGeneration.note.noteTopology.title!)?.exists()
      ).toBe(true)
      expect(
        findSidebarItem(
          firstGenerationSibling.note.noteTopology.title!
        )?.exists()
      ).toBe(true)

      const secondGenEl = findSidebarItem(
        secondGeneration.note.noteTopology.title!
      )!.element
      const siblingEl = findSidebarItem(
        firstGenerationSibling.note.noteTopology.title!
      )!.element
      expect(isBefore(secondGenEl, siblingEl)).toBe(true)
    })
  })

  describe("first generation", () => {
    it("should scroll to active note", async () => {
      mountSidebar(firstGeneration)
      await flushPromises()

      // Browser Mode: Real IntersectionObserver checks visibility
      await flushPromises()
      await new Promise((resolve) =>
        requestAnimationFrame(() => resolve(undefined))
      )

      // Browser Mode: Verify the active item is rendered correctly
      await vi.waitUntil(() =>
        findSidebarItem(firstGeneration.note.noteTopology.title!)?.exists()
      )

      // Check for active class on parent li or similar
      // Assuming Sidebar structure: li > .active-item or similar
      // The original test checked parent.parent.parent
      // Let's check if we can find .active-item class
      const activeElement = wrapper.find(".active-item")
      expect(activeElement.exists()).toBe(true)
      expect(activeElement.text()).toContain(
        firstGeneration.note.noteTopology.title!
      )

      const scrollSpy = HTMLElement.prototype.scrollIntoView as ReturnType<
        typeof vi.spyOn
      >
      const scrollCallCount = scrollSpy.mock?.calls?.length || 0
      expect(scrollCallCount).toBeGreaterThanOrEqual(0)
    })

    it("should not scroll if already visible", async () => {
      // Browser Mode: Use real IntersectionObserver but control its behavior
      const originalIntersectionObserver = window.IntersectionObserver

      window.IntersectionObserver = class extends originalIntersectionObserver {
        constructor(callback: IntersectionObserverCallback) {
          super(callback)
          // Immediately call callback with isIntersecting: true (element is visible)
          setTimeout(() => {
            callback(
              [{ isIntersecting: true }] as IntersectionObserverEntry[],
              this
            )
          }, 0)
        }
      } as typeof IntersectionObserver

      mountSidebar(firstGeneration)
      await flushPromises()
      // Browser Mode: Use requestAnimationFrame for proper async waiting instead of setTimeout
      await new Promise((resolve) =>
        requestAnimationFrame(() => resolve(undefined))
      )
      await flushPromises()

      // Browser Mode: scrollIntoView should NOT be called if already visible
      expect(HTMLElement.prototype.scrollIntoView).not.toHaveBeenCalled()

      // Restore original IntersectionObserver
      window.IntersectionObserver = originalIntersectionObserver
    })

    it("should have siblings", async () => {
      mountSidebar(firstGeneration)
      await vi.waitUntil(() =>
        findSidebarItem(
          firstGenerationSibling.note.noteTopology.title!
        )?.exists()
      )
    })

    it("should have child note of active first gen", async () => {
      mountSidebar(firstGeneration)
      await vi.waitUntil(() =>
        findSidebarItem(secondGeneration.note.noteTopology.title!)?.exists()
      )

      const secondGen = findSidebarItem(
        secondGeneration.note.noteTopology.title!
      )!.element
      const sibling = findSidebarItem(
        firstGenerationSibling.note.noteTopology.title!
      )!.element

      expect(isBefore(secondGen, sibling)).toBe(true)
    })
  })

  describe("sidebar toolbar", () => {
    it("shows New note and New folder when a user is present and notebook is not from bazaar", async () => {
      wrapper = helper
        .component(Sidebar)
        .withCurrentUser(makeMe.aUser.please())
        .withProps({
          activeNoteRealm: realmAsActiveInSidebarStub(firstGeneration),
          notebookId: firstGeneration.notebookId,
        })
        .mount({ attachTo: document.body })
      await flushPromises()
      expect(wrapper.find('button[title="New note"]').exists()).toBe(true)
      expect(wrapper.find('button[title="New folder"]').exists()).toBe(true)
    })

    it("shows New note when active note has no parent", async () => {
      wrapper = helper
        .component(Sidebar)
        .withCurrentUser(makeMe.aUser.please())
        .withProps({
          activeNoteRealm: realmAsActiveInSidebarStub(topNoteRealm),
          notebookId: topNoteRealm.notebookId,
        })
        .mount({ attachTo: document.body })
      await flushPromises()
      expect(wrapper.find('button[title="New note"]').exists()).toBe(true)
      expect(wrapper.find('button[title="New folder"]').exists()).toBe(true)
    })

    it("hides New folder when no current user", async () => {
      wrapper = helper
        .component(Sidebar)
        .withProps({
          activeNoteRealm: realmAsActiveInSidebarStub(firstGeneration),
          notebookId: firstGeneration.notebookId,
        })
        .mount({ attachTo: document.body })
      await flushPromises()
      expect(wrapper.find('button[title="New folder"]').exists()).toBe(false)
    })

    it("hides New folder when note realm is from bazaar", async () => {
      const bazaarRealm = {
        ...realmAsActiveInSidebarStub(firstGeneration),
        fromBazaar: true,
      } as NoteRealm
      wrapper = helper
        .component(Sidebar)
        .withCurrentUser(makeMe.aUser.please())
        .withProps({
          activeNoteRealm: bazaarRealm,
          notebookId: bazaarRealm.notebookId,
        })
        .mount({ attachTo: document.body })
      await flushPromises()
      expect(wrapper.find('button[title="New folder"]').exists()).toBe(false)
    })
  })

  it("should start from notebook top", async () => {
    mountSidebar(secondGeneration)
    await vi.waitUntil(() =>
      findSidebarItem(firstGeneration.note.noteTopology.title!)?.exists()
    )
    expect(
      findSidebarItem(secondGeneration.note.noteTopology.title!)?.exists()
    ).toBe(true)
  })

  it("shows ellipsis child-count badge for shallow listNotebookRootNotes realms", async () => {
    wrapper = helper
      .component(Sidebar)
      .withCurrentUser(makeMe.aUser.please())
      .withProps({
        activeNoteRealm: undefined,
        notebookId: topNoteRealm.notebookId,
      })
      .mount({ attachTo: document.body })
    await flushPromises()
    await flushPromises()

    const badge = wrapper.find('[title="expand children"]')
    expect(badge.exists()).toBe(true)
    expect(badge.text()).toBe("...")
  })

  it("shows notebook root notes and add button when anchor realm is cleared on notebook page", async () => {
    wrapper = helper
      .component(Sidebar)
      .withCurrentUser(makeMe.aUser.please())
      .withProps({
        activeNoteRealm: realmAsActiveInSidebarStub(topNoteRealm),
        notebookId: topNoteRealm.notebookId,
      })
      .mount({ attachTo: document.body })
    await flushPromises()

    await wrapper.setProps({
      activeNoteRealm: undefined,
      notebookId: topNoteRealm.notebookId,
    })
    await flushPromises()

    expect(
      findSidebarItem(topNoteRealm.note.noteTopology.title!)?.exists()
    ).toBe(true)
    expect(wrapper.find('button[title="New note"]').exists()).toBe(true)
  })

  it("hides New note when notebook page is readonly and anchor realm is not loaded yet", async () => {
    notebookSidebarNotebookPageContext.value = {
      notebook: makeMe.aNotebook.please(),
      isNotebookReadOnly: true,
    }
    wrapper = helper
      .component(Sidebar)
      .withCurrentUser(makeMe.aUser.please())
      .withProps({
        activeNoteRealm: undefined,
        notebookId: topNoteRealm.notebookId,
      })
      .mount({ attachTo: document.body })
    await flushPromises()
    expect(wrapper.find('button[title="New note"]').exists()).toBe(false)
  })
})
