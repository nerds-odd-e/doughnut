import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import FolderSelector from "@/components/notes/FolderSelector.vue"
import { mockCoarsePointer } from "@tests/helpers/mockCoarsePointer"
import { mockSdkService, testFolderStub } from "@tests/helpers"
import {
  mountSoftKeyboardPrimer,
  softKeyboardPrimerElement,
  waitUntilFocused,
} from "@tests/helpers/softKeyboardPrimerTestSupport"
import { flushPromises, mount, type VueWrapper } from "@vue/test-utils"
import { createRouter, createWebHistory } from "vue-router"
import routes from "@/routes/routes"
import { folderSearchResultTestId } from "@/utils/searchDialogKeyboard"
import {
  dispatchArrowKey,
  testIdSelector,
} from "@tests/helpers/searchDialogKeyboardTestSupport"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

const folderSearchResultSelector = testIdSelector(folderSearchResultTestId)

function allFolderSearchResults(): HTMLButtonElement[] {
  return Array.from(
    document.querySelectorAll<HTMLButtonElement>(folderSearchResultSelector)
  )
}

const router = createRouter({
  history: createWebHistory(),
  routes,
})

describe("FolderSelector", () => {
  const commonProps = {
    notebookId: 301,
    contextFolder: null,
    ancestorFolders: [],
    modelValue: null,
  }

  let matchMediaSpy: ReturnType<typeof mockCoarsePointer> | undefined
  let wrapper: VueWrapper | undefined

  beforeEach(() => {
    mockSdkService(NotebookController, "listNotebookFolderListing", {
      folders: [],
    })
    mockSdkService(NotebookController, "listNotebookFolderIndex", [
      testFolderStub(7, "Alpha"),
      testFolderStub(8, "Beta"),
    ])
  })

  afterEach(() => {
    matchMediaSpy?.mockRestore()
    matchMediaSpy = undefined
    wrapper?.unmount()
    document.body.innerHTML = ""
  })

  const mountSelector = () => {
    mountSoftKeyboardPrimer()
    wrapper = mount(FolderSelector, {
      props: commonProps,
      attachTo: document.body,
      global: {
        plugins: [router],
      },
    })
    return wrapper
  }

  const clickSearchMoreButton = () => {
    const button = document.querySelector(
      '[data-testid="folder-selector-more-button"]'
    ) as HTMLButtonElement | null
    expect(button).toBeTruthy()
    button!.click()
  }

  const clickSearchMoreButtonAndSettle = async () => {
    clickSearchMoreButton()
    await flushPromises()
    await wrapper?.vm.$nextTick()
  }

  describe("keyboard navigation", () => {
    async function openFolderSearchDialog() {
      matchMediaSpy = mockCoarsePointer(false)
      mountSelector()
      await flushPromises()
      await clickSearchMoreButtonAndSettle()
      await vi.waitUntil(
        () =>
          document.querySelector(
            '[data-testid="folder-selector-search-input"]'
          ) !== null,
        { timeout: 2000 }
      )
      return document.querySelector(
        '[data-testid="folder-selector-search-input"]'
      ) as HTMLInputElement
    }

    it("moves focus through folder results and back to search input with ArrowDown and ArrowUp", async () => {
      const searchInput = await openFolderSearchDialog()
      const results = allFolderSearchResults()
      expect(results.length).toBeGreaterThanOrEqual(2)

      const [firstResult, secondResult] = results

      dispatchArrowKey("ArrowDown", searchInput)
      expect(document.activeElement).toBe(firstResult)

      dispatchArrowKey("ArrowDown")
      expect(document.activeElement).toBe(secondResult)

      dispatchArrowKey("ArrowUp")
      expect(document.activeElement).toBe(firstResult)

      dispatchArrowKey("ArrowUp")
      expect(document.activeElement).toBe(searchInput)
    })
  })

  describe("soft keyboard primer", () => {
    it("focuses primer synchronously when search is opened on touch device", async () => {
      matchMediaSpy = mockCoarsePointer(true)
      mountSelector()
      await flushPromises()
      const primer = softKeyboardPrimerElement()
      expect(primer).toBeTruthy()

      clickSearchMoreButton()

      expect(document.activeElement).toBe(primer)
    })

    it("transfers focus to search input after folder index loads", async () => {
      matchMediaSpy = mockCoarsePointer(true)
      mountSelector()
      await flushPromises()

      await clickSearchMoreButtonAndSettle()

      await vi.waitUntil(
        () =>
          document.querySelector(
            '[data-testid="folder-selector-search-dialog"]'
          ) !== null,
        { timeout: 2000 }
      )

      await waitUntilFocused('[data-testid="folder-selector-search-input"]')
    })

    it("does not focus primer when pointer is not coarse", async () => {
      matchMediaSpy = mockCoarsePointer(false)
      mountSelector()
      await flushPromises()
      const primer = softKeyboardPrimerElement()

      await clickSearchMoreButtonAndSettle()

      expect(document.activeElement).not.toBe(primer)
      await waitUntilFocused('[data-testid="folder-selector-search-input"]')
    })
  })

  it("renders neighbour labels after the folder parent context changes", async () => {
    const alpha = testFolderStub(1, "Alpha")
    const beta = testFolderStub(2, "Beta")
    mockSdkService(NotebookController, "listNotebookFolderListing", {
      folders: [alpha],
    })

    wrapper = mount(FolderSelector, {
      props: {
        notebookId: 1,
        contextFolder: beta,
        ancestorFolders: [],
        modelValue: alpha,
      },
      attachTo: document.body,
      global: { plugins: [router] },
    })
    await flushPromises()

    await wrapper.setProps({ ancestorFolders: [alpha, beta] })
    await flushPromises()

    const alphaOption = wrapper
      .get('[data-testid="folder-move-parent-select"]')
      .find('option[value="1"]')
    expect(alphaOption.text()).toBe("Alpha")
  })
})
