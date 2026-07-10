import {
  NoteController,
  NotebookController,
  SearchController,
} from "@generated/doughnut-backend-api/sdk.gen"
import NoteNewForm from "@/components/notes/NoteNewForm.vue"
import type { ComponentPublicInstance } from "vue"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import { expect } from "vitest"

export const noteNewFormRealm = makeMe.aNoteRealm.title("mythical").please()
export const noteNewFormNote = noteNewFormRealm.note

export const notebookRootProps = {
  notebookId: noteNewFormRealm.notebookRealm.notebook.id,
  titleSearchAnchorNote: noteNewFormNote,
  ancestorFolders: noteNewFormRealm.ancestorFolders ?? [],
}

export type NoteNewFormSdkSpies = {
  searchForRelationshipTargetWithinSpy: ReturnType<typeof mockSdkService>
  semanticSearchWithinSpy: ReturnType<typeof mockSdkService>
  mockedCreateNoteAtRoot: ReturnType<typeof mockSdkService>
}

export function setupNoteNewFormSdkMocks(): NoteNewFormSdkSpies {
  mockSdkService(SearchController, "searchForRelationshipTarget", [])
  const searchForRelationshipTargetWithinSpy = mockSdkService(
    SearchController,
    "searchForRelationshipTargetWithin",
    []
  )
  mockSdkService(SearchController, "semanticSearch", [])
  const semanticSearchWithinSpy = mockSdkService(
    SearchController,
    "semanticSearchWithin",
    []
  )
  mockSdkService(NoteController, "getRecentNotes", [])
  mockSdkService(NotebookController, "listNotebookFolderIndex", [])
  mockSdkService(NotebookController, "listNotebookFolderListing", {
    folders: [],
  })
  const mockedCreateNoteAtRoot = mockSdkService(
    NotebookController,
    "createNoteAtNotebookRoot",
    makeMe.aNoteRealm.please()
  )
  return {
    searchForRelationshipTargetWithinSpy,
    semanticSearchWithinSpy,
    mockedCreateNoteAtRoot,
  }
}

export function mountNoteNewForm(
  props: Record<string, unknown> = notebookRootProps,
  options?: { attachTo?: HTMLElement }
) {
  const chain = helper
    .component(NoteNewForm)
    .withCleanStorage()
    .withProps(props)
  return options?.attachTo
    ? chain.mount({ attachTo: options.attachTo })
    : chain.mount()
}

export async function setNoteNewFormTitle(
  wrapper: VueWrapper<ComponentPublicInstance>,
  value: string
) {
  const el = wrapper.find('[data-test="note-title"]').element as HTMLElement
  el.innerText = value
  el.dispatchEvent(new Event("input", { bubbles: true }))
  await flushPromises()
}

export function noteTitleText(
  wrapper: VueWrapper<ComponentPublicInstance>
): string {
  return (wrapper.find('[data-test="note-title"]').element as HTMLElement)
    .innerText
}

export function wikidataModal(): HTMLElement | null {
  return document.querySelector(".modal-container")
}

export function wikidataDialogIsOpen(): boolean {
  return wikidataModal() !== null
}

export function wikidataSearchResultItem(wikidataId: string): HTMLElement {
  const item = wikidataModal()?.querySelector(
    `[data-testid="wikidata-search-result-item"][data-wikidata-id="${wikidataId}"]`
  ) as HTMLElement | null
  expect(item).toBeTruthy()
  return item!
}

export function wikidataCancelButton(): HTMLButtonElement {
  const button = wikidataModal()?.querySelector(
    "button.daisy-btn-secondary"
  ) as HTMLButtonElement | null
  expect(button).toBeTruthy()
  return button!
}

export async function openWikidataDialog(
  wrapper: VueWrapper<ComponentPublicInstance>,
  searchKey: string
) {
  await setNoteNewFormTitle(wrapper, searchKey)
  await wrapper.find("button[title='Wikidata Id']").trigger("click")
  await flushPromises()
}

export async function selectWikidataSearchResult(
  wikidataId: string,
  titleAction?: "Replace" | "Append"
) {
  wikidataSearchResultItem(wikidataId).click()
  await flushPromises()
  if (titleAction) {
    const label = wikidataModal()?.querySelector(
      `label[for="wikidataTitleAction-${titleAction}"]`
    ) as HTMLLabelElement | null
    expect(label).toBeTruthy()
    label!.click()
    await flushPromises()
  }
}

export function mockWikidataSearchResult(label: string, id: string) {
  return makeMe.aWikidataSearchEntity.label(label).id(id).please()
}

export function resolveWikidataSearch(
  searchWikidataSpy: ReturnType<typeof mockSdkService>,
  label: string,
  id: string
) {
  searchWikidataSpy.mockResolvedValue(
    wrapSdkResponse([mockWikidataSearchResult(label, id)])
  )
}
