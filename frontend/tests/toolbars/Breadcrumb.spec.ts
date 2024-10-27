import BreadcrumbWithCircle from "@/components/toolbars/BreadcrumbWithCircle.vue"
import { screen } from "@testing-library/vue"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"

describe("breadcrumb with circles", () => {
  const parentNote = makeMe.aNote.topicConstructor("parent").please()
  const child = makeMe.aNote
    .topicConstructor("child")
    .underNote(parentNote)
    .please()
  const grandChild = makeMe.aNote.underNote(child).please()

  it("render the breadcrumber", async () => {
    const wrapper = helper
      .component(BreadcrumbWithCircle)
      .withProps({ fromBazaar: false, noteTopic: parentNote.noteTopic })
      .mount()
    expect(wrapper.find(".breadcrumb-item").text()).toEqual("My Notes")
  })

  it("view note belongs to other people in bazaar", async () => {
    helper
      .component(BreadcrumbWithCircle)
      .withProps({ fromBazaar: true, noteTopic: parentNote.noteTopic })
      .render()
    await screen.findByText("Bazaar")
  })

  it("show ancestors in correct order", async () => {
    helper
      .component(BreadcrumbWithCircle)
      .withProps({ fromBazaar: false, noteTopic: grandChild.noteTopic })
      .render()
    const items = screen.getAllByText(/parent|child/)
    expect(items[0]).toHaveTextContent("parent")
    expect(items[1]).toHaveTextContent("child")
  })
})
