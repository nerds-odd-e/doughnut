import { WikidataController } from "@generated/doughnut-backend-api/sdk.gen"
import WikidataAssociationDialog from "@/components/notes/WikidataAssociationDialog.vue"
import { type VueWrapper, flushPromises } from "@vue/test-utils"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import { expect } from "vitest"

export type WikidataDialogSdkSpies = {
  searchWikidataSpy: ReturnType<typeof mockSdkService>
  fetchWikidataEntitySpy: ReturnType<typeof mockSdkService>
}

export function setupWikidataDialogSdkMocks(): WikidataDialogSdkSpies {
  return {
    searchWikidataSpy: mockSdkService(WikidataController, "searchWikidata", []),
    fetchWikidataEntitySpy: mockSdkService(
      WikidataController,
      "fetchWikidataEntityDataById",
      makeMe.aWikidataEntity.please()
    ),
  }
}

export type WikidataDialogMountOptions = {
  modelValue?: string
  errorMessage?: string
  showSaveButton?: boolean
  canSaveEmptyToClear?: boolean
  savedValue?: string
}

export function wikidataModal(): HTMLElement | null {
  return document.querySelector(".modal-container")
}

export function wikidataInput(): HTMLInputElement {
  const input = wikidataModal()?.querySelector(
    'input[id="wikidataID-wikidataID"]'
  ) as HTMLInputElement | null
  expect(input).toBeTruthy()
  return input!
}

export function wikidataSearchResults(): HTMLElement {
  const select = wikidataModal()?.querySelector(
    '[data-testid="wikidata-search-results"]'
  ) as HTMLElement | null
  expect(select).toBeTruthy()
  return select!
}

export function wikidataSearchResultItem(wikidataId: string): HTMLElement {
  const item = wikidataModal()?.querySelector(
    `[data-testid="wikidata-search-result-item"][data-wikidata-id="${wikidataId}"]`
  ) as HTMLElement | null
  expect(item).toBeTruthy()
  return item!
}

export function wikidataSaveButton(): HTMLButtonElement {
  const button = Array.from(
    wikidataModal()?.querySelectorAll("button") || []
  ).find((btn) => btn.textContent?.trim() === "Save") as
    | HTMLButtonElement
    | undefined
  expect(button).toBeTruthy()
  return button!
}

export function wikidataTitleActionLabel(
  action: "Replace" | "Append"
): HTMLLabelElement {
  const label = wikidataModal()?.querySelector(
    `label[for="wikidataTitleAction-${action}"]`
  ) as HTMLLabelElement | null
  expect(label).toBeTruthy()
  return label!
}

export function mockWikidataSearchResult(
  searchWikidataSpy: ReturnType<typeof mockSdkService>,
  label: string,
  id: string
) {
  const searchResult = makeMe.aWikidataSearchEntity.label(label).id(id).please()
  searchWikidataSpy.mockResolvedValue(wrapSdkResponse([searchResult]))
  return searchResult
}

export function mountWikidataAssociationDialog(
  searchKey: string,
  options?: WikidataDialogMountOptions
  // biome-ignore lint/suspicious/noExplicitAny: wrapper for testing
): VueWrapper<any> {
  return helper
    .component(WikidataAssociationDialog)
    .withProps({
      searchKey,
      ...options,
    })
    .mount({ attachTo: document.body })
}

export async function mountWikidataDialogReady(options: {
  searchWikidataSpy: ReturnType<typeof mockSdkService>
  searchKey: string
  searchLabel: string
  wikidataId: string
  mountOptions?: WikidataDialogMountOptions
}) {
  const searchResult = mockWikidataSearchResult(
    options.searchWikidataSpy,
    options.searchLabel,
    options.wikidataId
  )
  const wrapper = mountWikidataAssociationDialog(
    options.searchKey,
    options.mountOptions
  )
  await flushPromises()
  return { wrapper, searchResult }
}

export async function clickWikidataSearchResult(wikidataId: string) {
  wikidataSearchResultItem(wikidataId).click()
  await flushPromises()
}

export async function clickWikidataTitleAction(action: "Replace" | "Append") {
  wikidataTitleActionLabel(action).click()
  await flushPromises()
}

export async function selectWikidataSearchResultWithTitleAction(
  wikidataId: string,
  titleAction: "Replace" | "Append"
) {
  await clickWikidataSearchResult(wikidataId)
  await clickWikidataTitleAction(titleAction)
}

export function expectReplaceTitleAndAddAliasControls(suggestedLabel: string) {
  const modal = wikidataModal()
  expect(modal?.textContent).toContain(`Suggested Title: ${suggestedLabel}`)
  expect(modal?.textContent).toContain("Replace title")
  expect(modal?.textContent).toContain("Add as alias")
  expect(modal?.querySelector('input[value="Replace"]')).toBeTruthy()
  expect(modal?.querySelector('input[value="Append"]')).toBeTruthy()
}
