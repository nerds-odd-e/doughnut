import { NOTE_SIDEBAR_PEER_SORT_STORAGE_KEY } from "@/composables/useNoteSidebarPeerSort"
import Sidebar from "@/components/notes/Sidebar.vue"
import { invalidateSidebarListingCache } from "@/components/notes/sidebarFolderListingCache"
import type { ApiStatus } from "@/managedApi/ApiStatusHandler"
import {
  setupGlobalClient,
  teardownGlobalClientForTesting,
} from "@/managedApi/clientSetup"
import createNoteStorage from "@/store/createNoteStorage"
import type {
  FolderListing,
  NoteRealm,
  NotebookRealm,
  Options,
  ShowNoteData,
} from "@generated/doughnut-backend-api"
import {
  NoteController,
  NotebookController,
} from "@generated/doughnut-backend-api/sdk.gen"
import makeMe from "doughnut-test-fixtures/makeMe"
import {
  mockSdkServiceWithImplementation,
  testFolderStub,
} from "@tests/helpers"
import { type VueWrapper, DOMWrapper, flushPromises } from "@vue/test-utils"
import { computed, defineComponent } from "vue"
import { useRouter, type RouteLocationRaw } from "vue-router"
import { expect, vi } from "vitest"

/** SPA navigation only (no document navigation); matches real RouterLink enough for sidebar tests. */
const sidebarRouterLinkStub = defineComponent({
  name: "SidebarRouterLinkStub",
  props: {
    to: { type: [String, Object], required: true },
  },
  inheritAttrs: true,
  setup(props) {
    const router = useRouter()
    const href = computed(() => {
      try {
        return router.resolve(props.to as RouteLocationRaw).href
      } catch {
        return "#"
      }
    })
    async function onClick() {
      await router.push(props.to as RouteLocationRaw)
    }
    return { href, onClick }
  },
  template: `<a class="router-link" :href="href" @click.prevent="onClick"><slot /></a>`,
})

export function isBefore(node1: Node, node2: Node) {
  return !!(
    // eslint-disable-next-line no-bitwise
    (node1.compareDocumentPosition(node2) & Node.DOCUMENT_POSITION_FOLLOWING)
  )
}

type ListNotebookFolderListingOptions = Parameters<
  typeof NotebookController.listNotebookFolderListing
>[0]

export const EMPTY_FOLDER_LISTING: FolderListing = {
  noteTopologies: [],
  folders: [],
}

export function folderListingForQueryParent(
  options: unknown,
  table: Record<string, FolderListing>
): FolderListing {
  const query = (options as ListNotebookFolderListingOptions).query
  return table[String(query?.parent)] ?? EMPTY_FOLDER_LISTING
}

export function countFolderListingCallsForParent(
  listingSpy: { mock: { calls: unknown[][] } },
  parent: number | undefined
) {
  const want = String(parent)
  return listingSpy.mock.calls.filter(
    (call) =>
      String((call[0] as ListNotebookFolderListingOptions).query?.parent) ===
      want
  ).length
}

/** Distinct from note ids — folder listing API uses folder entity ids. */
export const FOLDER_TOP_NOTE_CHILDREN_ID = 77001
export const FOLDER_FIRST_GEN_CHILDREN_ID = 77002

export function structuralFolder(folderId: number, noteRealm: NoteRealm) {
  return testFolderStub(folderId, noteRealm.note.noteTopology.title)
}

export function mockShowNoteForRealms(realms: NoteRealm[]) {
  const byId = Object.fromEntries(realms.map((r) => [r.id, r])) as Record<
    number,
    NoteRealm
  >
  mockSdkServiceWithImplementation(NoteController, "showNote", (options) => {
    const id = (options as Options<ShowNoteData>).path.note
    const realm = byId[id]
    expect(
      realm,
      `sidebar tests: unmocked showNote for note id ${id}`
    ).toBeDefined()
    return realm!
  })
}

export function rootRowLabels(w: VueWrapper<unknown>): string[] {
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

export const DEFAULT_ROOT_PEER_ORDER = [
  "folder:banana",
  "folder:mango",
  "note:apple",
  "note:zebra",
] as const

export type SidebarTreeFixtures = {
  topNoteRealm: NoteRealm
  firstGeneration: NoteRealm
  firstGenerationSibling: NoteRealm
  secondGeneration: NoteRealm
  defaultTreeFolderListings: Record<string, FolderListing>
}

export function stubNotebookFolderListings(
  defaultTreeFolderListings: Record<string, FolderListing>
) {
  return mockSdkServiceWithImplementation(
    NotebookController,
    "listNotebookFolderListing",
    (options) => folderListingForQueryParent(options, defaultTreeFolderListings)
  )
}

export function findSidebarItem(
  wrapper: VueWrapper<unknown>,
  text: string
): DOMWrapper<Element> | undefined {
  const inner = wrapper
    .findAll(".title-text")
    .find((el) => el.text().includes(text))
  const li = inner?.element.closest("li")
  return li != null ? new DOMWrapper(li) : undefined
}

export function installSidebarDomMeasurementStubs(vi: {
  fn: typeof import("vitest").vi.fn
  spyOn: typeof import("vitest").vi.spyOn
}) {
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
}

export type SidebarTestHelper = typeof import("@tests/helpers").default

/** Mount first-generation sidebar and assert the active note row is in the DOM. */
export async function mountSidebarFirstGenReady(
  h: SidebarTestHelper,
  fixtures: SidebarTreeFixtures
) {
  const wrapper = mountSidebar(h, fixtures.firstGeneration)
  await flushPromises()
  const title = fixtures.firstGeneration.note.noteTopology.title
  const item = findSidebarItem(wrapper, title)
  expect(item, `sidebar note row "${title}"`).toBeDefined()
  expect(item!.exists()).toBe(true)
  return wrapper
}

export function mountSidebar(
  h: SidebarTestHelper,
  active: NoteRealm,
  notebookReadonly?: boolean
) {
  return h
    .component(Sidebar)
    .withRouter()
    .withProps({
      activeNoteRealm: active,
      notebookId: active.notebookRealm.notebook.id,
      notebookReadonly,
      breadcrumbFolders: active.ancestorFolders ?? [],
    })
    .mount({
      attachTo: document.body,
      global: {
        stubs: {
          RouterLink: sidebarRouterLinkStub,
          "router-link": sidebarRouterLinkStub,
        },
      },
    })
}

export function mountSidebarSignedIn(
  h: SidebarTestHelper,
  active: NoteRealm | undefined,
  notebookId: number,
  notebookReadonly?: boolean
) {
  return h
    .component(Sidebar)
    .withRouter()
    .withCurrentUser(makeMe.aUser.please())
    .withProps({
      activeNoteRealm: active,
      notebookId,
      notebookReadonly,
      breadcrumbFolders: active?.ancestorFolders ?? [],
    })
    .mount({
      attachTo: document.body,
      global: {
        stubs: {
          RouterLink: sidebarRouterLinkStub,
          "router-link": sidebarRouterLinkStub,
        },
      },
    })
}

/** Root list: two folders + zebra/apple notes; empty folder listings. */
type NoteStorageAccessor = ReturnType<
  typeof import("@/composables/useStorageAccessor")["useStorageAccessor"]
>

export function seedDefaultTreeRealmsInStorage(
  storageAccessor: NoteStorageAccessor,
  fixtures: SidebarTreeFixtures
) {
  storageAccessor.value.refOfNoteRealm(fixtures.topNoteRealm.id).value =
    fixtures.topNoteRealm
  storageAccessor.value.refOfNoteRealm(fixtures.firstGeneration.id).value =
    fixtures.firstGeneration
  storageAccessor.value.refOfNoteRealm(
    fixtures.firstGenerationSibling.id
  ).value = fixtures.firstGenerationSibling
  storageAccessor.value.refOfNoteRealm(fixtures.secondGeneration.id).value =
    fixtures.secondGeneration
}

export function setupDefaultSidebarSdkMocks(fixtures: SidebarTreeFixtures) {
  stubNotebookFolderListings(fixtures.defaultTreeFolderListings)
  mockShowNoteForRealms([
    fixtures.topNoteRealm,
    fixtures.firstGeneration,
    fixtures.firstGenerationSibling,
    fixtures.secondGeneration,
  ])
  mockSdkServiceWithImplementation(
    NotebookController,
    "getFolderPage",
    (options) => {
      type Opt = Parameters<typeof NotebookController.getFolderPage>[0]
      const { path } = options as Opt
      const nameById: Record<number, string> = {
        [FOLDER_TOP_NOTE_CHILDREN_ID]:
          fixtures.topNoteRealm.note.noteTopology.title,
        [FOLDER_FIRST_GEN_CHILDREN_ID]:
          fixtures.firstGeneration.note.noteTopology.title,
      }
      const title = nameById[path.folder] ?? `Folder #${path.folder}`
      return {
        ...makeMe.aFolderRealm.folder(path.folder, title).please(),
        notebookRealm: fixtures.topNoteRealm.notebookRealm,
      }
    }
  )
}

export function resetSidebarPeerSortSessionStorage() {
  sessionStorage.removeItem(NOTE_SIDEBAR_PEER_SORT_STORAGE_KEY)
}

export function prepareSidebarDefaultMountContext(options: {
  storageAccessor: NoteStorageAccessor
  fixtures: SidebarTreeFixtures
  vi: {
    fn: typeof import("vitest").vi.fn
    spyOn: typeof import("vitest").vi.spyOn
  }
}) {
  invalidateSidebarListingCache()
  resetSidebarPeerSortSessionStorage()
  options.storageAccessor.value = createNoteStorage()
  seedDefaultTreeRealmsInStorage(options.storageAccessor, options.fixtures)
  setupDefaultSidebarSdkMocks(options.fixtures)
  installSidebarDomMeasurementStubs(options.vi)
}

export function teardownSidebarComponentTest(
  wrapper: VueWrapper<unknown> | undefined
) {
  wrapper?.unmount()
  document.body.innerHTML = ""
  vi.restoreAllMocks()
}

export function neverResolving<T>(): Promise<T> {
  return new Promise(() => undefined)
}

export function stubShowNotePending() {
  mockSdkServiceWithImplementation(NoteController, "showNote", () =>
    neverResolving()
  )
}

export function uncachedNoteInSameNotebook(
  notebookRealm: NotebookRealm,
  title: string
): NoteRealm {
  const note = makeMe.aNoteRealm.title(title).please()
  note.notebookRealm = notebookRealm
  return note
}

export function sidebarShowsActiveItem(
  wrapper: VueWrapper<unknown>,
  noteTitle: string
): boolean {
  return (
    findSidebarItem(wrapper, noteTitle)?.element.classList.contains(
      "active-item"
    ) ?? false
  )
}

export async function withTrackingGlobalApiClient<T>(
  fn: (apiStatus: ApiStatus) => Promise<T>
): Promise<T> {
  const apiStatus: ApiStatus = { states: [] }
  setupGlobalClient(apiStatus)
  try {
    return await fn(apiStatus)
  } finally {
    teardownGlobalClientForTesting()
  }
}

export function setupRootPeersWithFolders(options: {
  storageAccessor: NoteStorageAccessor
  topNoteRealm: NoteRealm
  realmZ: NoteRealm
  realmA: NoteRealm
  folderExtras?: {
    mango?: Partial<ReturnType<typeof testFolderStub>>
    banana?: Partial<ReturnType<typeof testFolderStub>>
  }
}) {
  const { storageAccessor, topNoteRealm, realmZ, realmA, folderExtras } =
    options
  const nbId = topNoteRealm.notebookRealm.notebook.id
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
  mockSdkServiceWithImplementation(
    NotebookController,
    "listNotebookFolderListing",
    (options) => folderListingForQueryParent(options, rootPeersFolderListings)
  )
  mockShowNoteForRealms([topNoteRealm, realmZ, realmA])
  return { nbId, realmA, realmZ }
}
