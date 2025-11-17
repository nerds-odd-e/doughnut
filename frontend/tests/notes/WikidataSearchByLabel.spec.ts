import WikidataSearchByLabel from "@/components/notes/WikidataSearchByLabel.vue"
import helper from "@tests/helpers"

vitest.mock("vue-router", () => ({
  useRoute: () => ({
    path: "/",
  }),
}))

describe("WikidataSearchByLabel", () => {
  const mountComponent = (modelValue?: string) => {
    return helper
      .component(WikidataSearchByLabel)
      .withProps({
        searchKey: "test",
        modelValue,
      })
      .mount()
  }

  it("shows outline neutral button when wikidata id is empty", () => {
    const wrapper = mountComponent()
    const button = wrapper.find("button")
    expect(button.classes()).toContain("daisy-btn-outline")
    expect(button.classes()).toContain("daisy-btn-neutral")
    expect(button.classes()).not.toContain("daisy-btn-primary")
  })

  it("shows outline neutral button when wikidata id is undefined", () => {
    const wrapper = mountComponent(undefined)
    const button = wrapper.find("button")
    expect(button.classes()).toContain("daisy-btn-outline")
    expect(button.classes()).toContain("daisy-btn-neutral")
    expect(button.classes()).not.toContain("daisy-btn-primary")
  })

  it("shows primary button when wikidata id has value", () => {
    const wrapper = mountComponent("Q123")
    const button = wrapper.find("button")
    expect(button.classes()).toContain("daisy-btn-primary")
    expect(button.classes()).not.toContain("daisy-btn-outline")
    expect(button.classes()).not.toContain("daisy-btn-neutral")
  })

  it("shows outline neutral button when wikidata id is only whitespace", () => {
    const wrapper = mountComponent("   ")
    const button = wrapper.find("button")
    expect(button.classes()).toContain("daisy-btn-outline")
    expect(button.classes()).toContain("daisy-btn-neutral")
    expect(button.classes()).not.toContain("daisy-btn-primary")
  })

  it("updates button style when modelValue changes", async () => {
    const wrapper = mountComponent()
    const button = wrapper.find("button")

    expect(button.classes()).toContain("daisy-btn-outline")
    expect(button.classes()).toContain("daisy-btn-neutral")

    await wrapper.setProps({ modelValue: "Q456" })

    expect(button.classes()).toContain("daisy-btn-primary")
    expect(button.classes()).not.toContain("daisy-btn-outline")
    expect(button.classes()).not.toContain("daisy-btn-neutral")
  })
})
