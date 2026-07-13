import NoteNewForm from "@/components/notes/NoteNewForm.vue"
import { flushPromises } from "@vue/test-utils"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, { testFolderStub } from "@tests/helpers"
import {
  mountNoteNewForm,
  noteNewFormNote,
  noteNewFormRealm,
  notebookRootProps,
  setNoteNewFormTitle,
  setupNoteNewFormSdkMocks,
  type NoteNewFormSdkSpies,
} from "@tests/notes/noteNewFormTestSupport"
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

describe("NoteNewForm parent relationship", () => {
  let sdkSpies: NoteNewFormSdkSpies

  beforeEach(() => {
    vi.useFakeTimers()
    vi.resetAllMocks()
    popupsMock.confirm.mockReset()
    popupsMock.confirm.mockResolvedValue(false)
    sdkSpies = setupNoteNewFormSdkMocks()
  })

  afterEach(() => {
    vi.runOnlyPendingTimers()
    vi.useRealTimers()
  })

  it("submits parent frontmatter when Under current is selected", async () => {
    const wrapper = mountNoteNewForm(notebookRootProps, {
      attachTo: document.body,
    })
    await flushPromises()
    expect(
      wrapper.find('[data-testid="note-creation-parent-relationship"]').text()
    ).not.toContain("Same parent")
    await wrapper
      .find('label[for="note-creation-under_current"]')
      .trigger("click")
    await setNoteNewFormTitle(wrapper, "Child topic")
    await wrapper.find('[data-testid="note-new-form"]').trigger("submit")
    await flushPromises()
    expect(sdkSpies.mockedCreateNoteAtRoot).toHaveBeenCalledWith({
      path: { notebook: noteNewFormRealm.notebookRealm.notebook.id },
      body: expect.objectContaining({
        newTitle: "Child topic",
        content: expect.stringContaining('parent: "[[mythical]]"'),
      }),
    })
    wrapper.unmount()
  })

  it("submits copied parent when Same parent is selected", async () => {
    const noteWithParent = makeMe.aNoteRealm
      .title("team")
      .content('---\nparent: "[[Course intro]]"\n---\n')
      .please().note
    const wrapper = mountNoteNewForm(
      {
        notebookId: noteNewFormRealm.notebookRealm.notebook.id,
        titleSearchAnchorNote: noteWithParent,
        ancestorFolders: [],
      },
      { attachTo: document.body }
    )
    await flushPromises()
    expect(
      wrapper.find('[data-testid="note-creation-parent-relationship"]').text()
    ).toContain("Same parent")
    await wrapper
      .find('label[for="note-creation-same_parent"]')
      .trigger("click")
    await setNoteNewFormTitle(wrapper, "Sibling")
    await wrapper.find('[data-testid="note-new-form"]').trigger("submit")
    await flushPromises()
    expect(sdkSpies.mockedCreateNoteAtRoot).toHaveBeenCalledWith({
      path: { notebook: noteNewFormRealm.notebookRealm.notebook.id },
      body: expect.objectContaining({
        newTitle: "Sibling",
        content: expect.stringContaining('parent: "[[Course intro]]"'),
      }),
    })
    wrapper.unmount()
  })

  it("hides relationship options without a context note", async () => {
    const wrapper = mountNoteNewForm(
      {
        notebookId: noteNewFormRealm.notebookRealm.notebook.id,
        ancestorFolders: [],
      },
      { attachTo: document.body }
    )
    await flushPromises()
    expect(
      wrapper.find('[data-testid="note-creation-parent-relationship"]').exists()
    ).toBe(false)
    wrapper.unmount()
  })
})

describe("NoteNewForm folder label", () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.resetAllMocks()
    setupNoteNewFormSdkMocks()
  })

  afterEach(() => {
    vi.runOnlyPendingTimers()
    vi.useRealTimers()
  })

  it("shows folder dropdown label when initial folder is outside ancestorFolders", async () => {
    const wrapper = helper
      .component(NoteNewForm)
      .withCleanStorage()
      .withProps({
        notebookId: noteNewFormRealm.notebookRealm.notebook.id,
        titleSearchAnchorNote: noteNewFormNote,
        ancestorFolders: [],
        initialFolder: testFolderStub(99, "LeSS in Action"),
      })
      .mount({ attachTo: document.body })

    await flushPromises()

    const select = wrapper.find('[data-testid="folder-move-parent-select"]')
      .element as HTMLSelectElement
    const selectedText = select.selectedOptions[0]?.textContent?.trim() ?? ""
    expect(selectedText).toBe("LeSS in Action")

    wrapper.unmount()
  })
})
