import {
  NoteController,
  WikidataController,
} from "@generated/doughnut-backend-api/sdk.gen"
import { VueWrapper, flushPromises } from "@vue/test-utils"
import type { ComponentPublicInstance } from "vue"
import makeMe from "doughnut-test-fixtures/makeMe"
import { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import {
  mountNoteNewForm,
  mockWikidataSearchResult,
  noteTitleText,
  notebookRootProps,
  openWikidataDialog,
  resolveWikidataSearch,
  selectWikidataSearchResult,
  setNoteNewFormTitle,
  setupNoteNewFormSdkMocks,
  wikidataCancelButton,
  wikidataDialogIsOpen,
  type NoteNewFormSdkSpies,
} from "@tests/notes/noteNewFormTestSupport"
import { describe, it, expect, beforeEach, afterEach, vi } from "vitest"
import { RESERVED_README_TITLE_MESSAGE } from "@/utils/reservedReadmeTitles"

const popupsMock = {
  confirm: vi.fn().mockResolvedValue(false),
  alert: vi.fn(),
  options: vi.fn(),
  done: vi.fn(),
  register: vi.fn(),
  peek: vi.fn(),
}

vi.mock("@/components/commons/Popups/usePopups", () => ({
  default: () => ({ popups: popupsMock }),
}))

vi.mock("vue-router", async (importOriginal) => {
  const actual = await importOriginal<typeof import("vue-router")>()
  return {
    ...actual,
    useRouter: () => ({
      currentRoute: { value: {} },
    }),
    useRoute: () => ({ path: "/", fullPath: "/" }),
  }
})

describe("NoteNewForm wikidata and soft-delete", () => {
  let sdkSpies: NoteNewFormSdkSpies
  let wrapper: VueWrapper<ComponentPublicInstance>
  let searchWikidataSpy: ReturnType<typeof mockSdkService>

  beforeEach(() => {
    vi.useFakeTimers()
    vi.resetAllMocks()
    popupsMock.confirm.mockReset()
    popupsMock.confirm.mockResolvedValue(false)
    sdkSpies = setupNoteNewFormSdkMocks()
  })

  afterEach(() => {
    wrapper?.unmount()
    vi.runOnlyPendingTimers()
    vi.useRealTimers()
  })

  describe("submit errors", () => {
    beforeEach(async () => {
      wrapper = mountNoteNewForm(notebookRootProps, {
        attachTo: document.body,
      })
      await setNoteNewFormTitle(wrapper, "note title")
      vi.clearAllTimers()
    })

    it("displays reserved title error when api returns binding error for newTitle", async () => {
      await setNoteNewFormTitle(wrapper, "readme")

      sdkSpies.mockedCreateNoteAtRoot.mockResolvedValueOnce({
        data: undefined,
        error: {
          message: "binding error",
          errorType: "BINDING_ERROR",
          errors: {
            newTitle: RESERVED_README_TITLE_MESSAGE,
          },
        },
        request: {} as Request,
        response: { status: 400, url: "" } as Response,
        // biome-ignore lint/suspicious/noExplicitAny: SDK error result shape
      } as any)

      await wrapper.find('[data-testid="note-new-form"]').trigger("submit")
      await flushPromises()

      expect(wrapper.text()).toContain("reserved")
    })

    it("asks confirmation on soft-deleted title conflict and calls undo delete when confirmed", async () => {
      popupsMock.confirm.mockResolvedValueOnce(true)
      const restoredRealm = makeMe.aNoteRealm.please()
      const undoSpy = mockSdkService(
        NoteController,
        "undoDeleteNote",
        restoredRealm
      )
      sdkSpies.mockedCreateNoteAtRoot.mockResolvedValueOnce({
        data: undefined,
        error: {
          message:
            "A note with this title already exists here but was deleted.",
          errorType: "SOFT_DELETED_TITLE_CONFLICT",
          errors: { deletedNoteId: "99" },
        },
        request: {} as Request,
        response: { status: 409, url: "" } as Response,
        // biome-ignore lint/suspicious/noExplicitAny: SDK error result shape
      } as any)

      await wrapper.find('[data-testid="note-new-form"]').trigger("submit")
      await flushPromises()

      expect(popupsMock.confirm).toHaveBeenCalledWith(
        expect.stringContaining("deleted")
      )
      expect(undoSpy).toHaveBeenCalledWith({ path: { note: 99 } })
    })
  })

  describe("search wikidata entry", () => {
    beforeEach(() => {
      vi.useRealTimers()
      sdkSpies.searchForRelationshipTargetWithinSpy.mockResolvedValue(
        wrapSdkResponse([])
      )
      searchWikidataSpy = mockSdkService(
        WikidataController,
        "searchWikidata",
        []
      )
      wrapper = mountNoteNewForm(notebookRootProps, {
        attachTo: document.body,
      })
    })

    afterEach(() => {
      vi.useFakeTimers()
    })

    it("opens wikidata dialog on search and closes on cancel", async () => {
      resolveWikidataSearch(searchWikidataSpy, "dog", "Q1")
      await openWikidataDialog(wrapper, "dog")
      expect(searchWikidataSpy).toHaveBeenCalledWith({
        query: { search: "dog" },
      })
      expect(wikidataDialogIsOpen()).toBe(true)

      wikidataCancelButton().click()
      await flushPromises()

      expect(wikidataDialogIsOpen()).toBe(false)
    })

    it.each`
      searchTitle | wikidataTitle | wikidataId | titleAction  | expectedTitle
      ${"dog"}    | ${"dog"}      | ${"Q1"}    | ${undefined} | ${"dog"}
      ${"dog"}    | ${"Dog"}      | ${"Q1"}    | ${undefined} | ${"Dog"}
      ${"dog"}    | ${"Canine"}   | ${"Q1"}    | ${"replace"} | ${"Canine"}
      ${"dog"}    | ${"Canine"}   | ${"Q1"}    | ${"append"}  | ${"dog"}
    `(
      "search $searchTitle get $wikidataTitle with action $titleAction updates title as $expectedTitle",
      async ({
        searchTitle,
        wikidataTitle,
        wikidataId,
        titleAction,
        expectedTitle,
      }) => {
        searchWikidataSpy.mockResolvedValue(
          wrapSdkResponse([mockWikidataSearchResult(wikidataTitle, wikidataId)])
        )
        await openWikidataDialog(wrapper, searchTitle)
        await selectWikidataSearchResult(
          wikidataId,
          titleAction
            ? ((titleAction.charAt(0).toUpperCase() + titleAction.slice(1)) as
                | "Replace"
                | "Append")
            : undefined
        )

        expect(wikidataDialogIsOpen()).toBe(false)
        expect(noteTitleText(wrapper)).toBe(expectedTitle)
      }
    )
  })
})
