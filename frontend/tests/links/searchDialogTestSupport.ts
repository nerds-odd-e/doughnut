import {
  NoteController,
  SearchController,
} from "@generated/doughnut-backend-api/sdk.gen"
import type { Note } from "@generated/doughnut-backend-api"
import SearchForm from "@/components/links/SearchForm.vue"
import Modal from "@/components/commons/Modal.vue"
import usePopups from "@/components/commons/Popups/usePopups"
import { fireEvent, screen } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import MakeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import {
  appendSearchKeyToHistory,
  clearSearchKeyHistoryCookie,
} from "@/utils/searchKeyHistoryCookie"
import { searchResultItemTestId } from "@/utils/searchDialogKeyboard"
import { testIdSelector } from "@tests/helpers/searchDialogKeyboardTestSupport"
import { advanceSearchDebounce } from "@tests/helpers/searchDebounceTestSupport"
import { beforeEach, expect, vi } from "vitest"
import { defineComponent } from "vue"

export const searchResultItemSelector = testIdSelector(searchResultItemTestId)

export const deadLinkPayload = {
  targetToken: "original text",
  displayText: "original text",
} as const

export function allSearchResultItems(): Element[] {
  return Array.from(document.querySelectorAll(searchResultItemSelector))
}

export function titleEl(title: string): HTMLElement {
  const el = document.querySelector(`[title="${title}"]`)
  expect(el).toBeTruthy()
  return el as HTMLElement
}

export function historyDropdown(): HTMLDetailsElement {
  return screen.getByTestId("search-key-history-dropdown") as HTMLDetailsElement
}

export function makeNoteHit(title: string, notebookId: number) {
  return {
    hitKind: "NOTE" as const,
    noteSearchResult: MakeMe.aNoteSearchResult
      .title(title)
      .notebookId(notebookId)
      .please(),
  }
}

export function makeFolderHit(folderId: number, folderName: string) {
  return {
    hitKind: "FOLDER" as const,
    folderId,
    folderName,
    notebookId: 1,
    notebookName: "Nb",
    distance: 0.9,
  }
}

export function makeNotebookHit(notebookId: number, notebookName: string) {
  return {
    hitKind: "NOTEBOOK" as const,
    notebookId,
    notebookName,
    distance: 0,
  }
}

export function setupSearchFormSdkMocks() {
  mockSdkService(NoteController, "getRecentNotes", [])
  mockSdkService(SearchController, "searchForRelationshipTarget", [])
  mockSdkService(SearchController, "searchForRelationshipTargetWithin", [])
  mockSdkService(SearchController, "semanticSearch", [])
  mockSdkService(SearchController, "semanticSearchWithin", [])
}

export async function typeInSearch(input: HTMLElement, value: string) {
  fireEvent.update(input, value)
  await advanceSearchDebounce()
}

export async function renderSearchForm(
  props: {
    note?: Note | null
    deadLinkPayload?: {
      targetToken: string
      displayText: string
    }
  },
  options?: { router?: boolean; cleanStorage?: boolean }
) {
  let chain = helper.component(SearchForm)
  if (options?.cleanStorage !== false) {
    chain = chain.withCleanStorage()
  }
  if (options?.router) {
    chain = chain.withRouter()
  }
  chain.withProps(props).render()
  await flushPromises()
  return screen.getByPlaceholderText("Search")
}

export async function renderSearchWithKeyHistory(
  note: Note,
  keys: string[] = ["older"]
) {
  for (const key of keys) {
    appendSearchKeyToHistory(key)
  }
  helper.component(SearchForm).withCleanStorage().withProps({ note }).render()
  await flushPromises()
  return screen.getByPlaceholderText("Search")
}

export async function openSearchKeyHistoryDropdown() {
  fireEvent.click(screen.getByTestId("search-key-history-trigger"))
  await flushPromises()
  expect(historyDropdown().open).toBe(true)
}

export async function confirmMovePopup() {
  usePopups().popups.done(true)
  await flushPromises()
}

export async function openAddLinkChoice(
  note: Note,
  options?: { router?: boolean }
) {
  mockSdkService(SearchController, "searchForRelationshipTargetWithin", [
    makeNoteHit("Target Note", note.noteTopology.id + 100),
  ])
  const searchInput = await renderSearchForm({ note }, options)
  await typeInSearch(searchInput, "Target")
  fireEvent.click(screen.getByText("Add link"))
  await flushPromises()
}

export async function searchAndClickMoveUnder(note: Note, targetFolderId = 42) {
  mockSdkService(SearchController, "searchForRelationshipTargetWithin", [
    makeFolderHit(targetFolderId, "Archive"),
  ])
  const searchInput = await renderSearchForm({ note })
  await typeInSearch(searchInput, "Arc")
  fireEvent.click(screen.getByText("Move Under"))
  await flushPromises()
}

export async function renderSearchFormInModal(note: Note) {
  const SearchFormInModal = defineComponent({
    components: { Modal, SearchForm },
    props: ["note"],
    template: `
      <Modal :show-close-button="false">
        <template #body>
          <SearchForm :note="note" />
        </template>
      </Modal>
    `,
  })

  helper
    .component(SearchFormInModal)
    .withCleanStorage()
    .withRouter()
    .withProps({ note })
    .render()
  await flushPromises()
  return screen.getByPlaceholderText("Search")
}

export function setupSearchDialogTests() {
  beforeEach(() => {
    vi.clearAllMocks()
    clearSearchKeyHistoryCookie()
    setupSearchFormSdkMocks()
  })
}
