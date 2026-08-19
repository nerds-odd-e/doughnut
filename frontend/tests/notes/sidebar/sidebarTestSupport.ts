import { PEER_SORT_STORAGE_KEY } from "@/composables/usePeerSort"
import { invalidateSidebarListingCache } from "@/components/notes/sidebarFolderListingCache"
import type { ApiStatus } from "@/managedApi/ApiStatusHandler"
import {
  setupGlobalClient,
  teardownGlobalClientForTesting,
} from "@/managedApi/clientSetup"
import createNoteStorage from "@/store/createNoteStorage"
import type { NoteRealm, NotebookRealm } from "@generated/doughnut-backend-api"
import {
  NoteController,
  NotebookController,
} from "@generated/doughnut-backend-api/sdk.gen"
import makeMe from "doughnut-test-fixtures/makeMe"
import { mockSdkServiceWithImplementation } from "@tests/helpers"
import { type VueWrapper } from "@vue/test-utils"
import { vi } from "vitest"
import {
  FOLDER_FIRST_GEN_CHILDREN_ID,
  FOLDER_TOP_NOTE_CHILDREN_ID,
  mockShowNoteForRealms,
  stubNotebookFolderListings,
  type SidebarTreeFixtures,
} from "./sidebarFolderListingSupport"

export * from "./sidebarFolderListingSupport"
export * from "./sidebarMountSupport"

type NoteStorageAccessor = ReturnType<
  typeof import("@/composables/useStorageAccessor")["useStorageAccessor"]
>

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

/** Browser IntersectionObserver stub — fires once on observe with a fixed intersecting flag. */
export function stubIntersectionObserver(isIntersecting: boolean): () => void {
  const original = globalThis.IntersectionObserver
  globalThis.IntersectionObserver = class {
    constructor(private readonly cb: IntersectionObserverCallback) {}
    observe() {
      setTimeout(() => {
        this.cb(
          [{ isIntersecting }] as IntersectionObserverEntry[],
          this as unknown as IntersectionObserver
        )
      }, 0)
    }
    disconnect() {
      /* no-op stub */
    }
    unobserve() {
      /* no-op stub */
    }
  } as unknown as typeof IntersectionObserver
  return () => {
    globalThis.IntersectionObserver = original
  }
}

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

export function resetPeerSortStorage() {
  sessionStorage.removeItem(PEER_SORT_STORAGE_KEY)
  localStorage.removeItem(PEER_SORT_STORAGE_KEY)
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
  resetPeerSortStorage()
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
  return makeMe.aNoteRealm
    .title(title)
    .inNotebook(notebookRealm.notebook.id, notebookRealm.notebook.name)
    .please()
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
