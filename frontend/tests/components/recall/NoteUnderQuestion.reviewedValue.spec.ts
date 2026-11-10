import AnsweredQuestionComponent from "@/components/recall/AnsweredQuestionComponent.vue"
import MemoryTrackerPageView from "@/pages/MemoryTrackerPageView.vue"
import NoteUnderQuestion from "@/components/recall/NoteUnderQuestion.vue"
import {
  notePropertyItemLocation,
  notePropertyLocation,
  noteShowLocation,
} from "@/routes/noteShowLocation"
import { wikiLinkFromAuthoredToken } from "@/utils/authoredLinkMarkup"
import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import type { WikiLink } from "@generated/donut-backend-api"
import helper, { mockSdkService, productionRouterAt } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import { expect, it } from "vitest"

async function mountReviewedValue(value: string, wikiLinks: WikiLink[] = []) {
  const realm = makeMe.aNoteRealm
    .title("Sentence")
    .content(
      `---\nexample of: ${JSON.stringify([value, "[[walk]]"])}\n---\nBody.`
    )
    .wikiLinks(wikiLinks)
    .please()
  mockSdkService(NoteController, "showNote", realm)
  const router = await productionRouterAt({ name: "root" })
  const wrapper = helper
    .component(NoteUnderQuestion)
    .withCleanStorage()
    .withRealRouter(router)
    .withProps({
      noteTopology: realm.note.noteTopology,
      focusedPropertyKey: "example of",
      focusedPropertyValue: value,
    })
    .mount()
  await flushPromises()
  return { wrapper, router, realm }
}

it("shows a plain reviewed value as readable text", async () => {
  const { wrapper } = await mountReviewedValue("run")
  const value = wrapper.get('[data-testid="reviewed-property-value"]')
  expect(value.text()).toBe(": run")
  expect(value.element.querySelector("a")).toBeNull()
  const breadcrumb = wrapper.get(".breadcrumb-wrapper").element
  const property = wrapper.get(
    '[data-testid="focused-property-indicator"]'
  ).element
  expect(
    breadcrumb.compareDocumentPosition(property) &
      Node.DOCUMENT_POSITION_FOLLOWING
  ).toBeTruthy()
  wrapper.unmount()
})

it.each([
  { raw: "[[run]]", label: "run", destination: noteShowLocation(77) },
  {
    raw: "[[Verbs/run|running]]",
    label: "running",
    destination: noteShowLocation(77),
  },
  {
    raw: "[[Verbs/run#prop:tense|past]]",
    label: "past",
    destination: notePropertyLocation(77, "tense"),
  },
])(
  "follows the readable $raw reference independently of its reviewed source",
  async ({ raw, label, destination }) => {
    const { wrapper, router, realm } = await mountReviewedValue(raw, [
      wikiLinkFromAuthoredToken(raw.slice(2, -2), 77),
    ])
    const value = wrapper.get(
      '[data-testid="reviewed-property-value"] a.donut-wiki-link'
    )
    expect(value.text()).toBe(label)
    await wrapper.get('[data-testid="reviewed-property-link"]').trigger("click")
    await flushPromises()
    expect(router.currentRoute.value).toMatchObject(
      notePropertyItemLocation(realm.id, "example of", {
        value: raw,
        occurrence: 1,
      })
    )
    await value.trigger("click")
    await flushPromises()
    expect(router.currentRoute.value).toMatchObject(destination)
    wrapper.unmount()
  }
)

it("keeps a saved unresolved reviewed reference visibly unresolved", async () => {
  const { wrapper, router } = await mountReviewedValue("[[Missing|unknown]]")
  const link = wrapper.get(
    '[data-testid="reviewed-property-value"] a.dead-wiki-link'
  )
  expect(link.text()).toBe("unknown")
  expect(link.attributes("href")).toBe("#")
  await link.trigger("click")
  await flushPromises()
  expect(router.currentRoute.value.name).toBe("root")
  wrapper.unmount()
})

it("keeps the tracker source item identity independent of a readable alias", async () => {
  const raw = "[[Verbs/run|running]]"
  const realm = makeMe.aNoteRealm
    .content(`---\nexample of: ${JSON.stringify([raw])}\n---`)
    .wikiLinks([wikiLinkFromAuthoredToken("Verbs/run|running", 77)])
    .please()
  mockSdkService(NoteController, "showNote", realm)
  const router = await productionRouterAt({ name: "root" })
  const wrapper = helper
    .component(MemoryTrackerPageView)
    .withCleanStorage()
    .withRealRouter(router)
    .withProps({
      memoryTracker: makeMe.aMemoryTracker
        .ofNote(realm)
        .withPropertyKey("example of", raw)
        .please(),
      memoryTrackerId: 1,
      recallHistory: [],
    })
    .mount()
  await flushPromises()
  expect(wrapper.get('[data-testid="reviewed-property-value"] a').text()).toBe(
    "running"
  )
  await wrapper.get('[data-testid="reviewed-property-link"]').trigger("click")
  await flushPromises()
  expect(router.currentRoute.value).toMatchObject(
    notePropertyItemLocation(realm.id, "example of", {
      value: raw,
      occurrence: 1,
    })
  )
  wrapper.unmount()
})

it("opens the authored key for key-only recall from a nullable wire response", async () => {
  const realm = makeMe.aNoteRealm.content("---\ntopic: movement\n---").please()
  mockSdkService(NoteController, "showNote", realm)
  const answeredQuestion = JSON.parse(
    JSON.stringify(
      makeMe.anAnsweredQuestion
        .withNote(realm.note)
        .withPropertyKey("topic")
        .please()
    )
  )
  answeredQuestion.recalledNote.propertyValue = null
  const router = await productionRouterAt({ name: "root" })
  const wrapper = helper
    .component(AnsweredQuestionComponent)
    .withCleanStorage()
    .withRealRouter(router)
    .withProps({ answeredQuestion, conversationButton: false })
    .mount()
  await flushPromises()
  await wrapper.get('[data-testid="reviewed-property-link"]').trigger("click")
  await flushPromises()
  expect(router.currentRoute.value).toMatchObject(
    notePropertyLocation(realm.id, "topic")
  )
  wrapper.unmount()
})
