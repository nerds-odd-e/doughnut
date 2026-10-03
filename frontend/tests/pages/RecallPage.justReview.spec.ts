import {
  MemoryTrackerController,
  NoteController,
} from "@generated/donut-backend-api/sdk.gen"
import { notePropertyLocation } from "@/routes/noteShowLocation"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService, wrapSdkError } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { describe, expect, it, onTestFinished, vi } from "vitest"
import { page, server } from "vitest/browser"
import {
  createMemoryTrackerLite,
  givenRecallQueue,
  useRecallPageSpecContext,
} from "./recallPageTestSupport"

vi.mock("@/components/commons/Popups/usePopups")

vi.mock("vue-router", async (importOriginal) => {
  const actual = await importOriginal<typeof import("vue-router")>()
  return {
    ...actual,
    useRoute: () => ({ path: "/", fullPath: "/" }),
    useRouter: () => ({ currentRoute: { value: { name: "recall" } } }),
  }
})

const ctx = useRecallPageSpecContext()

describe("RecallPage Just review", () => {
  it("reads the complete note in place and grades it with reachable controls", async () => {
    const original = server.config.browser.viewport
    onTestFinished(() => page.viewport(original.width, original.height))
    await page.viewport(390, 600)
    const noteRealm = makeMe.aNoteRealm
      .title("Sedition")
      .content(`Inciting rebellion. ${"Long explanation. ".repeat(400)}`)
      .please()
    noteRealm.references = [
      makeMe.aNoteRealm.title("Treason").please().note.noteTopology,
    ]
    mockSdkService(NoteController, "showNote", noteRealm)
    mockSdkService(
      MemoryTrackerController,
      "showMemoryTracker",
      makeMe.aMemoryTracker.ofNote(noteRealm).please()
    )
    vi.mocked(MemoryTrackerController.getRecallPrompt).mockResolvedValue(
      wrapSdkError("No recall prompt")
    )
    givenRecallQueue(createMemoryTrackerLite(123), createMemoryTrackerLite(456))
    const markAsRecalled = mockSdkService(
      MemoryTrackerController,
      "markAsRecalled",
      makeMe.aMemoryTracker.please()
    )
    const host = document.createElement("div")
    host.style.cssText = "display: grid; grid-template-rows: 100vh"
    document.body.appendChild(host)

    const wrapper = ctx.renderer
      .currentRoute({ name: "recall" })
      .mount({ attachTo: host })
    await flushPromises()

    const context = document.querySelector('article[aria-label="Note context"]')
    expect(context?.textContent).toContain("Sedition")
    expect(context?.textContent).toContain("Inciting rebellion.")
    expect(context?.textContent).toContain("Treason")
    expect(wrapper.find("#main-note-content").exists()).toBe(false)
    const again = wrapper.find("button.daisy-btn-secondary")
    expect(again.text()).toBe("Again")
    expect(again.element.getBoundingClientRect().bottom).toBeLessThanOrEqual(
      window.innerHeight
    )

    await again.trigger("click")
    await flushPromises()
    expect(markAsRecalled).toHaveBeenCalledTimes(1)
    expect(markAsRecalled).toHaveBeenCalledWith({
      path: { memoryTracker: 123 },
      query: { grade: "AGAIN" },
    })
  })

  it("identifies a tracked property in the complete note and opens it on its note route", async () => {
    const noteRealm = makeMe.aNoteRealm
      .title("Sedition")
      .content("---\ncolor: red\nsize: big\n---\nInciting rebellion.")
      .please()
    mockSdkService(NoteController, "showNote", noteRealm)
    mockSdkService(
      MemoryTrackerController,
      "showMemoryTracker",
      makeMe.aMemoryTracker.ofNote(noteRealm).withPropertyKey("color").please()
    )
    vi.mocked(MemoryTrackerController.getRecallPrompt).mockResolvedValue(
      wrapSdkError("No recall prompt")
    )
    givenRecallQueue(createMemoryTrackerLite(123))

    const wrapper = ctx.renderer.currentRoute({ name: "recall" }).mount()
    await flushPromises()

    const context = wrapper.find('article[aria-label="Note context"]')
    expect(context.text()).toContain("Inciting rebellion.")
    expect(
      context
        .find('[data-property-key="color"]')
        .attributes("data-property-focused")
    ).toBe("true")
    expect(
      context
        .find('[data-property-key="size"]')
        .attributes("data-property-focused")
    ).toBeUndefined()
    expect(
      context
        .findAll("a")
        .find((a) => a.text() === "Open full note")
        ?.attributes("to")
    ).toBe(JSON.stringify(notePropertyLocation(noteRealm.id, "color")))
  })
})
