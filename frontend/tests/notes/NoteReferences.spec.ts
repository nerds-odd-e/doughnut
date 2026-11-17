import NoteReferences from "@/components/notes/NoteReferences.vue"
import makeMe from "donut-test-fixtures/makeMe"
import helper from "@tests/helpers"
import { describe, expect, it } from "vitest"

describe("NoteReferences", () => {
  const target = makeMe.aNoteRealm.title("Target").please()
  makeMe.aNoteRealm
    .title("Source")
    .referencing(target, "prerequisite", "related")
    .please()
  makeMe.aNoteRealm.title("Other").referencing(target).please()

  const mountReferences = () =>
    helper
      .component(NoteReferences)
      .withRouter()
      .withProps({ noteTopologies: target.references })
      .mount()

  const cardTitled = (
    wrapper: ReturnType<typeof mountReferences>,
    title: string
  ) => wrapper.findAll('[role="card"]').find((c) => c.text().includes(title))!

  it("shows the referring note's property types together as secondary text on its card", () => {
    const label = cardTitled(mountReferences(), "Source").get(
      "[data-reference-property-types]"
    )

    expect(label.text()).toBe("via its prerequisite, related")
    expect(label.classes()).toEqual(
      expect.arrayContaining(["text-sm", "truncate"])
    )
    expect(label.attributes("title")).toBe("via its prerequisite, related")
  })

  it("shows no label for a reference made without a property", () => {
    const card = cardTitled(mountReferences(), "Other")

    expect(card.find("[data-reference-property-types]").exists()).toBe(false)
  })

  it("keeps the collapsed count of references", async () => {
    const wrapper = mountReferences()

    await wrapper.get('button[title="collapse children"]').trigger("click")

    expect(wrapper.get('[role="collapsed-children-count"]').text()).toBe("2")
  })
})
