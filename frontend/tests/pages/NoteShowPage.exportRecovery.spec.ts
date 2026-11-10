import { afterEach, expect, it, vi } from "vitest"
import { cleanup } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import {
  NoteController,
  NoteExportController,
} from "@generated/donut-backend-api/sdk.gen"
import {
  mockSdkService,
  productionRouterAt,
  wrapSdkError,
  wrapSdkResponse,
} from "@tests/helpers"
import {
  renderNoteShowPageWithoutSidebar,
  setupNoteShowPageMocks,
  mainNoteContentEl,
} from "./noteShowPageTestSupport"
import {
  notePropertyLocation,
  notePropertyItemLocation,
  noteShowLocation,
} from "@/routes/noteShowLocation"
import {
  aiMarkdownTextarea,
  copyButton,
  downloadButton,
} from "../notes/noteExportFormTestSupport"
import { propertyRowSelector } from "../components/form/propertiesTestDom"

vi.mock("@/models/audio/recorderWorklet", async () =>
  (
    await import("../notes/noteVoiceInputButtonMocks")
  ).recorderWorkletMockExports()
)
vi.mock("@/models/audio/audioRecorder", async () =>
  (
    await import("../notes/noteVoiceInputButtonMocks")
  ).audioRecorderMockExports()
)
vi.mock("@/models/wakeLocker", async () =>
  (await import("../notes/noteVoiceInputButtonMocks")).wakeLockerMockExports()
)

afterEach(cleanup)
const realm = makeMe.aNoteRealm
  .content(
    "---\ntopic: Motion\nexample of: [run, run, walk]\n---\nReadable body."
  )
  .please()
const unresolvedFocusMessage =
  "The highlighted property or item cannot be resolved. Close export, select an existing focus or clear the highlight, then reopen."
async function openExport() {
  document.dispatchEvent(
    new KeyboardEvent("keydown", {
      key: "e",
      code: "KeyE",
      bubbles: true,
      cancelable: true,
    })
  )
  await flushPromises()
}
it.each(["select", "clear"] as const)(
  "recovers an unresolved export using the note's %s control",
  async (recovery) => {
    setupNoteShowPageMocks(realm)
    mockSdkService(NoteController, "getNoteInfo", { memoryTrackers: [] })
    const api = mockSdkService(NoteExportController, "getAiContextMarkdown", {
      markdown: "ordinary context",
    })
    const stale =
      recovery === "select"
        ? notePropertyLocation(realm.id, "missing")
        : notePropertyItemLocation(realm.id, "example of", {
            value: "gone",
            occurrence: 2,
          })
    const router = await productionRouterAt(stale)
    await renderNoteShowPageWithoutSidebar(router, realm.id)
    api.mockResolvedValueOnce(
      wrapSdkError({ message: unresolvedFocusMessage, status: 400 })
    )
    await openExport()
    expect(
      document.querySelector('[data-testid="export-error"]')?.textContent
    ).toContain(unresolvedFocusMessage)
    expect(copyButton().disabled).toBe(true)
    expect(downloadButton().disabled).toBe(true)
    document.querySelector<HTMLButtonElement>("dialog .close-button")!.click()
    await flushPromises()
    const key = recovery === "select" ? "topic" : "example of"
    mainNoteContentEl()!
      .querySelector<HTMLButtonElement>(
        `${propertyRowSelector(key)} [data-testid="rich-note-property-panel-toggle"]`
      )!
      .click()
    await flushPromises()
    expect(router.currentRoute.value).toMatchObject(
      recovery === "select"
        ? notePropertyLocation(realm.id, "topic")
        : noteShowLocation(realm.id)
    )
    const markdown =
      recovery === "select"
        ? "Property key: topic\nProperty value: Motion"
        : "Whole note context: Readable body."
    api.mockResolvedValueOnce(wrapSdkResponse({ markdown }))
    await openExport()
    expect(api).toHaveBeenLastCalledWith({
      path: { note: realm.id },
      query: {
        tokenLimit: 2000,
        ...(recovery === "select" ? { propertyKey: "topic" } : {}),
      },
    })
    expect(aiMarkdownTextarea()?.value).toBe(markdown)
    expect(copyButton().disabled).toBe(false)
  }
)
