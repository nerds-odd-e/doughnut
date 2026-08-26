import {
  NoteController,
  WikidataController,
} from "@generated/doughnut-backend-api/sdk.gen"
import { VueWrapper, flushPromises } from "@vue/test-utils"
import type { ComponentPublicInstance } from "vue"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import {
  mountNoteNewForm,
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
import { RESERVED_README_TITLE_MESSAGE } from "@/utils/reservedReadmeTitles"
import { describe, it, expect, beforeEach, afterEach, vi } from "vitest"

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
    vi.resetAllMocks()
    popupsMock.confirm.mockReset()
    popupsMock.confirm.mockResolvedValue(false)
    sdkSpies = setupNoteNewFormSdkMocks()
  })

  afterEach(() => {
    wrapper?.unmount()
  })

  describe("submit errors", () => {
    beforeEach(async () => {
      vi.useFakeTimers()
      wrapper = mountNoteNewForm(notebookRootProps, {
        attachTo: document.body,
      })
      await setNoteNewFormTitle(wrapper, "note title")
    })

    afterEach(() => {
      vi.runOnlyPendingTimers()
      vi.useRealTimers()
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
      vi.useFakeTimers()
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
      vi.runOnlyPendingTimers()
      vi.useRealTimers()
    })

    it("opens dialog, cancels, then applies matching-title selections", async () => {
      resolveWikidataSearch(searchWikidataSpy, "dog", "Q1")
      await openWikidataDialog(wrapper, "dog")
      expect(searchWikidataSpy).toHaveBeenCalledWith({
        query: { search: "dog" },
      })
      expect(wikidataDialogIsOpen()).toBe(true)

      wikidataCancelButton().click()
      await flushPromises()
      expect(wikidataDialogIsOpen()).toBe(false)

      await openWikidataDialog(wrapper, "dog")
      await selectWikidataSearchResult("Q1")
      expect(wikidataDialogIsOpen()).toBe(false)
      expect(noteTitleText(wrapper)).toBe("dog")

      resolveWikidataSearch(searchWikidataSpy, "Dog", "Q1")
      await openWikidataDialog(wrapper, "dog")
      await selectWikidataSearchResult("Q1")
      expect(wikidataDialogIsOpen()).toBe(false)
      expect(noteTitleText(wrapper)).toBe("Dog")
    })

    it("replace then append title actions update title for differing wikidata label", async () => {
      resolveWikidataSearch(searchWikidataSpy, "Canine", "Q1")
      await openWikidataDialog(wrapper, "dog")
      await selectWikidataSearchResult("Q1", "Replace")
      expect(noteTitleText(wrapper)).toBe("Canine")

      await setNoteNewFormTitle(wrapper, "dog")
      await openWikidataDialog(wrapper, "dog")
      await selectWikidataSearchResult("Q1", "Append")
      expect(noteTitleText(wrapper)).toBe("dog")
    })
  })
})
