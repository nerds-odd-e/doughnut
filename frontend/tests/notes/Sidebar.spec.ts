import Sidebar from "@/components/notes/Sidebar.vue"
import { notebookSidebarNotebookClientView } from "@/composables/useCurrentNoteSidebarState"
import { NOTE_SIDEBAR_PEER_SORT_STORAGE_KEY } from "@/composables/useNoteSidebarPeerSort"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import type {
  FolderListing,
  NoteRealm,
  Options,
  ShowNoteData,
} from "@generated/doughnut-backend-api"
import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import createNoteStorage from "@/store/createNoteStorage"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, {
  mockSdkServiceWithImplementation,
  testFolderStub,
} from "@tests/helpers"
import { type VueWrapper, DOMWrapper, flushPromises } from "@vue/test-utils"
import { vi, describe, it, expect, beforeEach, afterEach } from "vitest"

function isBefore(node1: Node, node2: Node) {
  return !!(
    // eslint-disable-next-line no-bitwise
    (node1.compareDocumentPosition(node2) & Node.DOCUMENT_POSITION_FOLLOWING)
  )
}

type ListNotebookFolderListingOptions = Parameters<
  typeof NotebookController.listNotebookFolderListing
>[0]

const EMPTY_FOLDER_LISTING: FolderListing = {
  noteTopologies: [],
  folders: [],
}

function folderListingForQueryParent(
  options: unknown,
  table: Record<string, FolderListing>
): FolderListing {
  const query = (options as ListNotebookFolderListingOptions).query
  return table[String(query?.parent)] ?? EMPTY_FOLDER_LISTING
}

/** Distinct from note ids — folder listing API uses folder entity ids. */
const FOLDER_TOP_NOTE_CHILDREN_ID = 77001
const FOLDER_FIRST_GEN_CHILDREN_ID = 77002

function structuralFolder(folderId: number, noteRealm: NoteRealm) {
  return testFolderStub(folderId, noteRealm.note.noteTopology.title)
}

/** Aligns listing payloads with folder-first sidebar drag rules (same folderId = same drop column). */
function noteTopologyInFolder(realm: NoteRealm, folderId: number) {
  return {
    ...realm.note.noteTopology,
    folderId,
  }
}

function mockShowNoteForRealms(realms: NoteRealm[]) {
  const byId = Object.fromEntries(realms.map((r) => [r.id, r])) as Record<
    number,
    NoteRealm
  >
  mockSdkServiceWithImplementation("showNote", (options) => {
    const id = (options as Options<ShowNoteData>).path.note
    const realm = byId[id]
    expect(
      realm,
      `Sidebar.spec: unmocked showNote for note id ${id}`
    ).toBeDefined()
    return realm!
  })
}

function rootRowLabels(w: VueWrapper<unknown>): string[] {
  const rootUl = w.get("ul.sidebar-tree-list")
  return Array.from(rootUl.element.children).map((li) => {
    const folderText = li
      .querySelector(".sidebar-folder-label")
      ?.textContent?.trim()
    const noteText = li.querySelector(".title-text")?.textContent?.trim()
    return folderText
      ? `folder:${folderText}`
      : noteText
        ? `note:${noteText}`
        : "?"
  })
}

const DEFAULT_ROOT_PEER_ORDER = [
  "folder:banana",
  "folder:mango",
  "note:apple",
  "note:zebra",
] as const

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

  const defaultTreeFolderListings: Record<string, FolderListing> = {
    [String(undefined)]: {
      noteTopologies: [
        {
          ...topNoteRealm.note.noteTopology,
          folderId: topNoteRealm.note.noteTopology.folderId ?? 0,
        },
      ],
      folders: [structuralFolder(FOLDER_TOP_NOTE_CHILDREN_ID, topNoteRealm)],
    },
    [String(FOLDER_TOP_NOTE_CHILDREN_ID)]: {
      noteTopologies: [
        noteTopologyInFolder(firstGeneration, FOLDER_TOP_NOTE_CHILDREN_ID),
        noteTopologyInFolder(
          firstGenerationSibling,
          FOLDER_TOP_NOTE_CHILDREN_ID
        ),
      ],
      folders: [
        structuralFolder(FOLDER_FIRST_GEN_CHILDREN_ID, firstGeneration),
      ],
    },
    [String(FOLDER_FIRST_GEN_CHILDREN_ID)]: {
      noteTopologies: [
        noteTopologyInFolder(secondGeneration, FOLDER_FIRST_GEN_CHILDREN_ID),
      ],
      folders: [],
    },
  }

  function stubNotebookFolderListings() {
    return mockSdkServiceWithImplementation(
      "listNotebookFolderListing",
      (options) =>
        folderListingForQueryParent(options, defaultTreeFolderListings)
    )
  }

  /** Folder context keyed by realm id — ancestorFolders and optional folderId override. */
  type FolderContext = {
    ancestorFolders: ReturnType<typeof testFolderStub>[]
    folderId?: number
  }
  const folderContextByRealmId: Record<number, FolderContext> = {
    [topNoteRealm.id]: { ancestorFolders: [] },
    [firstGeneration.id]: {
      ancestorFolders: [
        testFolderStub(
          FOLDER_TOP_NOTE_CHILDREN_ID,
          topNoteRealm.note.noteTopology.title
        ),
      ],
      folderId: FOLDER_TOP_NOTE_CHILDREN_ID,
    },
    [firstGenerationSibling.id]: {
      ancestorFolders: [
        testFolderStub(
          FOLDER_TOP_NOTE_CHILDREN_ID,
          topNoteRealm.note.noteTopology.title
        ),
      ],
      folderId: FOLDER_TOP_NOTE_CHILDREN_ID,
    },
    [secondGeneration.id]: {
      ancestorFolders: [
        testFolderStub(
          FOLDER_TOP_NOTE_CHILDREN_ID,
          topNoteRealm.note.noteTopology.title
        ),
        testFolderStub(
          FOLDER_FIRST_GEN_CHILDREN_ID,
          firstGeneration.note.noteTopology.title
        ),
      ],
      folderId: FOLDER_FIRST_GEN_CHILDREN_ID,
    },
  }

  /** Aligns active realm with folder-first API: ancestorFolders + folderId for this stub tree. */
  function realmAsActiveInSidebarStub(realm: NoteRealm): NoteRealm {
    const ctx = folderContextByRealmId[realm.id] ?? {
      ancestorFolders: realm.ancestorFolders ?? [],
    }
    const noteTopology =
      ctx.folderId !== undefined
        ? { ...realm.note.noteTopology, folderId: ctx.folderId }
        : realm.note.noteTopology
    return {
      ...realm,
      ancestorFolders: ctx.ancestorFolders,
      note: { ...realm.note, noteTopology },
    } as NoteRealm
  }

  const mountSidebar = (n: NoteRealm) => {
    wrapper = helper
      .component(Sidebar)
      .withProps({
        activeNoteRealm: realmAsActiveInSidebarStub(n),
        notebookId: n.notebookView.notebook.id,
      })
      .mount({ attachTo: document.body })
    return wrapper
  }

  /** Signed-in user + optional notebook id (defaults from active realm). When active is undefined, notebookId is required. */
  function mountSidebarSignedIn(
    active: NoteRealm | undefined,
    notebookId?: number
  ) {
    const nb =
      notebookId ??
      (active !== undefined
        ? active.notebookView.notebook.id
        : (() => {
            throw new Error("notebookId is required when active is undefined")
          })())
    wrapper = helper
      .component(Sidebar)
      .withCurrentUser(makeMe.aUser.please())
      .withProps({
        activeNoteRealm:
          active !== undefined ? realmAsActiveInSidebarStub(active) : undefined,
        notebookId: nb,
      })
      .mount({ attachTo: document.body })
    return wrapper
  }

  /** Root list: two folders + zebra/apple notes; empty folder listings. */
  function setupRootPeersWithFolders(
    realmZ: NoteRealm,
    realmA: NoteRealm,
    folderExtras?: {
      mango?: Partial<ReturnType<typeof testFolderStub>>
      banana?: Partial<ReturnType<typeof testFolderStub>>
    }
  ) {
    const nbId = topNoteRealm.notebookView.notebook.id
    storageAccessor.value.refOfNoteRealm(realmZ.id).value = realmZ
    storageAccessor.value.refOfNoteRealm(realmA.id).value = realmA
    storageAccessor.value.refOfNoteRealm(topNoteRealm.id).value = topNoteRealm

    const folderBanana = {
      ...testFolderStub(9001, "banana"),
      ...folderExtras?.banana,
    }
    const folderMango = {
      ...testFolderStub(9002, "mango"),
      ...folderExtras?.mango,
    }

    const rootPeersFolderListings: Record<string, FolderListing> = {
      [String(undefined)]: {
        noteTopologies: [realmZ.note.noteTopology, realmA.note.noteTopology],
        folders: [folderMango, folderBanana],
      },
    }
    mockSdkServiceWithImplementation("listNotebookFolderListing", (options) =>
      folderListingForQueryParent(options, rootPeersFolderListings)
    )
    mockShowNoteForRealms([topNoteRealm, realmZ, realmA])
    return { nbId, realmA, realmZ }
  }

  beforeEach(() => {
    sessionStorage.removeItem(NOTE_SIDEBAR_PEER_SORT_STORAGE_KEY)
    storageAccessor.value = createNoteStorage()
    storageAccessor.value.refOfNoteRealm(topNoteRealm.id).value = topNoteRealm
    storageAccessor.value.refOfNoteRealm(firstGeneration.id).value =
      firstGeneration
    storageAccessor.value.refOfNoteRealm(firstGenerationSibling.id).value =
      firstGenerationSibling
    storageAccessor.value.refOfNoteRealm(secondGeneration.id).value =
      secondGeneration

    stubNotebookFolderListings()
    mockShowNoteForRealms([
      topNoteRealm,
      firstGeneration,
      firstGenerationSibling,
      secondGeneration,
    ])

    Element.prototype.getBoundingClientRect = vi.fn().mockReturnValue({
      top: 0,
      bottom: 100,
      height: 100,
      width: 200,
      left: 0,
      right: 200,
      x: 0,
      y: 0,
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
    vi.spyOn(HTMLElement.prototype, "scrollIntoView")
  })

  afterEach(() => {
    notebookSidebarNotebookClientView.value = undefined
    wrapper?.unmount()
    document.body.innerHTML = ""
    vi.restoreAllMocks()
  })

  const findSidebarItem = (text: string): DOMWrapper<Element> | undefined => {
    const inner = wrapper
      .findAll(".title-text")
      .find((el) => el.text().includes(text))
    const li = inner?.element.closest("li")
    return li != null ? new DOMWrapper(li) : undefined
  }

  describe("user active folder", () => {
    const findRootFolderRowByTopTitle = () => {
      const label = wrapper
        .findAll(".sidebar-folder-label")
        .find((w) => w.text().includes(topNoteRealm.note.noteTopology.title))
      const li = label?.element.closest("li")
      return li != null ? new DOMWrapper(li) : undefined
    }

    async function mountFirstGenSidebarAndWaitNoteRow() {
      mountSidebar(firstGeneration)
      await vi.waitUntil(() =>
        findSidebarItem(firstGeneration.note.noteTopology.title)?.exists()
      )
    }

    it("applies sidebar-folder-user-active when a folder row is clicked", async () => {
      await mountFirstGenSidebarAndWaitNoteRow()
      const folderRow = findRootFolderRowByTopTitle()
      expect(folderRow?.exists()).toBe(true)
      await folderRow!.find(".sidebar-folder-label").trigger("click")
      await flushPromises()
      expect(folderRow!.classes()).toContain("sidebar-folder-user-active")
    })

    it("activates folder but does not toggle expand when only the folder label track padding is clicked", async () => {
      await mountFirstGenSidebarAndWaitNoteRow()
      const folderRow = findRootFolderRowByTopTitle()
      expect(folderRow?.exists()).toBe(true)
      const expandedBefore = folderRow!.attributes("aria-expanded")
      const track = folderRow!.find(".folder-label-area")
      expect(track.exists()).toBe(true)
      await track.trigger("click")
      await flushPromises()
      expect(folderRow!.classes()).toContain("sidebar-folder-user-active")
      expect(folderRow!.attributes("aria-expanded")).toBe(expandedBefore)
    })

    it("clears user active folder styling when a note row is clicked", async () => {
      mountSidebar(firstGeneration)
      await vi.waitUntil(() =>
        findSidebarItem(topNoteRealm.note.noteTopology.title)?.exists()
      )
      const folderRow = findRootFolderRowByTopTitle()
      await folderRow!.find(".sidebar-folder-label").trigger("click")
      await flushPromises()
      expect(folderRow!.classes()).toContain("sidebar-folder-user-active")

      const noteRow = findSidebarItem(topNoteRealm.note.noteTopology.title)
      expect(noteRow?.exists()).toBe(true)
      await noteRow!.trigger("click")
      await flushPromises()
      expect(folderRow!.classes()).not.toContain("sidebar-folder-user-active")
    })

    it("does not clear user active folder when modal opens after toolbar click (Safari behavior)", async () => {
      mountSidebarSignedIn(firstGeneration)
      await flushPromises()
      await vi.waitUntil(() =>
        findSidebarItem(firstGeneration.note.noteTopology.title)?.exists()
      )
      const folderRow = findRootFolderRowByTopTitle()
      await folderRow!.find(".sidebar-folder-label").trigger("click")
      await flushPromises()
      expect(folderRow!.classes()).toContain("sidebar-folder-user-active")

      const toolbarBtn = wrapper.find('button[title="New note"]')
      expect(toolbarBtn.exists()).toBe(true)
      toolbarBtn.element.dispatchEvent(
        new MouseEvent("mousedown", { bubbles: true })
      )
      const modalInput = document.createElement("input")
      document.body.appendChild(modalInput)
      folderRow!.element.focus()
      folderRow!.element.dispatchEvent(
        new FocusEvent("focusout", {
          bubbles: true,
          relatedTarget: modalInput,
        })
      )
      await flushPromises()
      document.body.removeChild(modalInput)
      expect(folderRow!.classes()).toContain("sidebar-folder-user-active")
    })

    it("does not clear user active folder when focus moves to the sidebar toolbar", async () => {
      mountSidebarSignedIn(firstGeneration)
      await flushPromises()
      await vi.waitUntil(() =>
        findSidebarItem(firstGeneration.note.noteTopology.title)?.exists()
      )
      const folderRow = findRootFolderRowByTopTitle()
      await folderRow!.find(".sidebar-folder-label").trigger("click")
      await flushPromises()
      const toolbarBtn = wrapper.find('button[title="New folder"]')
      expect(toolbarBtn.exists()).toBe(true)
      folderRow!.element.focus()
      folderRow!.element.dispatchEvent(
        new FocusEvent("focusout", {
          bubbles: true,
          relatedTarget: toolbarBtn.element,
        })
      )
      await flushPromises()
      expect(folderRow!.classes()).toContain("sidebar-folder-user-active")
    })
  })

  describe("sidebar peer sort", () => {
    it("shows sort control in the sidebar toolbar", async () => {
      mountSidebarSignedIn(topNoteRealm)
      await flushPromises()
      expect(wrapper.find("[data-note-sidebar-sort]").exists()).toBe(true)
    })

    it("lists folders above notes (A–Z) and reorders root peers when Title (Z–A) is chosen", async () => {
      storageAccessor.value = createNoteStorage()
      const realmZ = makeMe.aNoteRealm
        .title("zebra")
        .under(topNoteRealm)
        .please()
      const realmA = makeMe.aNoteRealm
        .title("apple")
        .under(topNoteRealm)
        .please()
      const { nbId, realmA: activeA } = setupRootPeersWithFolders(
        realmZ,
        realmA
      )

      mountSidebarSignedIn(activeA, nbId)
      await flushPromises()
      await vi.waitUntil(
        () => wrapper.findAll(".sidebar-folder-label").length >= 2
      )

      expect(rootRowLabels(wrapper)).toEqual([...DEFAULT_ROOT_PEER_ORDER])

      await wrapper.find("[data-note-sidebar-sort] summary").trigger("click")
      await flushPromises()
      await wrapper.find('button[title="Title (Z–A)"]').trigger("click")
      await flushPromises()

      expect(rootRowLabels(wrapper)).toEqual([
        "folder:mango",
        "folder:banana",
        "note:zebra",
        "note:apple",
      ])
    })

    it("applies sort from sessionStorage on mount without opening the menu", async () => {
      sessionStorage.setItem(
        NOTE_SIDEBAR_PEER_SORT_STORAGE_KEY,
        JSON.stringify({ field: "updated", direction: "desc" })
      )
      storageAccessor.value = createNoteStorage()
      const realmZ = makeMe.aNoteRealm
        .title("zebra")
        .updatedAt("2015-06-01T00:00:00.000Z")
        .under(topNoteRealm)
        .please()
      const realmA = makeMe.aNoteRealm
        .title("apple")
        .updatedAt("2020-01-01T00:00:00.000Z")
        .under(topNoteRealm)
        .please()
      setupRootPeersWithFolders(realmZ, realmA, {
        banana: { updatedAt: "2018-01-01T00:00:00.000Z" },
        mango: { updatedAt: "2005-01-01T00:00:00.000Z" },
      })

      mountSidebarSignedIn(realmA, topNoteRealm.notebookView.notebook.id)
      await flushPromises()
      await vi.waitUntil(
        () => wrapper.findAll(".sidebar-folder-label").length >= 2
      )

      expect(rootRowLabels(wrapper)).toEqual([...DEFAULT_ROOT_PEER_ORDER])
    })
  })

  it("should call the api once if top note", async () => {
    mountSidebar(topNoteRealm)
    await flushPromises()
    const topTitle = topNoteRealm.note.noteTopology.title
    const folderRowLabel = wrapper
      .findAll(".sidebar-folder-label")
      .find((w) => w.text().trim() === topTitle)
    expect(folderRowLabel?.exists()).toBe(true)
    await folderRowLabel!.trigger("click")
    await flushPromises()
    await vi.waitUntil(() =>
      findSidebarItem(firstGeneration.note.noteTopology.title)?.exists()
    )
  })

  it("does not reload notebook root notes when active note changes within the same notebook", async () => {
    const listingSpy = mockSdkServiceWithImplementation(
      "listNotebookFolderListing",
      (options) =>
        folderListingForQueryParent(options, defaultTreeFolderListings)
    )
    mountSidebar(firstGeneration)
    await flushPromises()
    const rootRequestCount = () =>
      listingSpy.mock.calls.filter(
        (call) =>
          String(
            (call[0] as ListNotebookFolderListingOptions).query?.parent
          ) === String(undefined)
      ).length
    expect(rootRequestCount()).toBe(1)
    expect(
      findSidebarItem(topNoteRealm.note.noteTopology.title)?.exists()
    ).toBe(true)

    await wrapper.setProps({
      activeNoteRealm: realmAsActiveInSidebarStub(secondGeneration),
      notebookId: firstGeneration.notebookView.notebook.id,
    })
    await flushPromises()
    expect(rootRequestCount()).toBe(1)
    expect(
      findSidebarItem(topNoteRealm.note.noteTopology.title)?.exists()
    ).toBe(true)
  })

  describe("gradual ancestor population", () => {
    beforeEach(() => {
      storageAccessor.value = createNoteStorage()
    })

    it("loads ancestor branches for a deep note through folder listings without showNote", async () => {
      const listingSpy = mockSdkServiceWithImplementation(
        "listNotebookFolderListing",
        (options) =>
          folderListingForQueryParent(options, defaultTreeFolderListings)
      )
      const showNoteSpy = mockSdkServiceWithImplementation("showNote", () => {
        throw new Error("Sidebar must not use showNote for structural branches")
      })

      mountSidebar(secondGeneration)
      await flushPromises()

      expect(
        listingSpy.mock.calls.filter(
          (call) =>
            String(
              (call[0] as ListNotebookFolderListingOptions).query?.parent
            ) === String(undefined)
        ).length
      ).toBe(1)
      expect(showNoteSpy).not.toHaveBeenCalled()
      const listedParentIds = listingSpy.mock.calls
        .map(
          (call) => (call[0] as { query?: { parent?: number } }).query?.parent
        )
        .filter((id): id is number => id !== undefined)
      expect(listedParentIds).toContain(FOLDER_TOP_NOTE_CHILDREN_ID)
      expect(listedParentIds).toContain(FOLDER_FIRST_GEN_CHILDREN_ID)
      expect(listedParentIds).not.toContain(firstGenerationSibling.id)

      await vi.waitUntil(() =>
        findSidebarItem(secondGeneration.note.noteTopology.title)?.exists()
      )
      expect(
        findSidebarItem(topNoteRealm.note.noteTopology.title)?.exists()
      ).toBe(true)
      expect(
        findSidebarItem(firstGeneration.note.noteTopology.title)?.exists()
      ).toBe(true)
      expect(
        findSidebarItem(
          firstGenerationSibling.note.noteTopology.title
        )?.exists()
      ).toBe(true)

      const secondGenEl = findSidebarItem(
        secondGeneration.note.noteTopology.title
      )!.element
      const siblingEl = findSidebarItem(
        firstGenerationSibling.note.noteTopology.title
      )!.element
      expect(isBefore(secondGenEl, siblingEl)).toBe(true)
    })
  })

  describe("first generation", () => {
    it("should scroll to active note", async () => {
      mountSidebar(firstGeneration)
      await flushPromises()
      await new Promise((resolve) =>
        requestAnimationFrame(() => resolve(undefined))
      )
      await vi.waitUntil(() =>
        findSidebarItem(firstGeneration.note.noteTopology.title)?.exists()
      )
      const activeElement = wrapper.find(".active-item")
      expect(activeElement.exists()).toBe(true)
      expect(activeElement.text()).toContain(
        firstGeneration.note.noteTopology.title
      )
    })

    it("should not scroll if already visible", async () => {
      const originalIntersectionObserver = window.IntersectionObserver
      window.IntersectionObserver = class extends originalIntersectionObserver {
        constructor(callback: IntersectionObserverCallback) {
          super(callback)
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
      await new Promise((resolve) =>
        requestAnimationFrame(() => resolve(undefined))
      )
      await flushPromises()
      expect(HTMLElement.prototype.scrollIntoView).not.toHaveBeenCalled()
      window.IntersectionObserver = originalIntersectionObserver
    })

    it("should have siblings", async () => {
      mountSidebar(firstGeneration)
      await vi.waitUntil(() =>
        findSidebarItem(
          firstGenerationSibling.note.noteTopology.title
        )?.exists()
      )
    })

    it("should have child note of active first gen", async () => {
      mountSidebar(firstGeneration)
      await flushPromises()
      const firstTitle = firstGeneration.note.noteTopology.title
      const nestedFolderLabel = wrapper
        .findAll(".sidebar-folder-label")
        .find((w) => w.text().trim() === firstTitle)
      expect(nestedFolderLabel?.exists()).toBe(true)
      await nestedFolderLabel!.trigger("click")
      await flushPromises()
      await vi.waitUntil(() =>
        findSidebarItem(secondGeneration.note.noteTopology.title)?.exists()
      )

      const secondGen = findSidebarItem(
        secondGeneration.note.noteTopology.title
      )!.element
      const sibling = findSidebarItem(
        firstGenerationSibling.note.noteTopology.title
      )!.element
      expect(isBefore(secondGen, sibling)).toBe(true)
    })
  })

  describe("sidebar toolbar", () => {
    it("shows New note and New folder when signed in and notebook is not from bazaar", async () => {
      mountSidebarSignedIn(topNoteRealm)
      await flushPromises()
      expect(wrapper.find('button[title="New note"]').exists()).toBe(true)
      expect(wrapper.find('button[title="New folder"]').exists()).toBe(true)
    })

    it("hides New folder when no current user", async () => {
      mountSidebar(firstGeneration)
      await flushPromises()
      expect(wrapper.find('button[title="New folder"]').exists()).toBe(false)
    })

    it("hides New folder when note realm is from bazaar", async () => {
      const bazaarRealm = {
        ...firstGeneration,
        notebookView: { ...firstGeneration.notebookView, readonly: true },
      } as NoteRealm
      mountSidebarSignedIn(bazaarRealm)
      await flushPromises()
      expect(wrapper.find('button[title="New folder"]').exists()).toBe(false)
    })
  })

  it("should start from notebook top", async () => {
    mountSidebar(secondGeneration)
    await vi.waitUntil(() =>
      findSidebarItem(firstGeneration.note.noteTopology.title)?.exists()
    )
    expect(
      findSidebarItem(secondGeneration.note.noteTopology.title)?.exists()
    ).toBe(true)
  })

  it("shows folder expand control at notebook root with shallow folder listing", async () => {
    mountSidebarSignedIn(undefined, topNoteRealm.notebookView.notebook.id)
    await flushPromises()
    await flushPromises()
    expect(wrapper.find('button[aria-label="expand children"]').exists()).toBe(
      true
    )
  })

  it("shows notebook root notes and add button when anchor realm is cleared on notebook page", async () => {
    mountSidebarSignedIn(topNoteRealm)
    await flushPromises()
    await wrapper.setProps({
      activeNoteRealm: undefined,
      notebookId: topNoteRealm.notebookView.notebook.id,
    })
    await flushPromises()
    expect(
      findSidebarItem(topNoteRealm.note.noteTopology.title)?.exists()
    ).toBe(true)
    expect(wrapper.find('button[title="New note"]').exists()).toBe(true)
  })

  it("hides New note when notebook page is readonly and anchor realm is not loaded yet", async () => {
    notebookSidebarNotebookClientView.value = {
      notebook: makeMe.aNotebook.please(),
      readonly: true,
    }
    mountSidebarSignedIn(undefined, topNoteRealm.notebookView.notebook.id)
    await flushPromises()
    expect(wrapper.find('button[title="New note"]').exists()).toBe(false)
  })
})
