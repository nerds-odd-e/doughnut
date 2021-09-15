import { screen } from "@testing-library/vue"
import NoteOverview from "@/components/notes/NoteOverview.vue"
import { renderWithMockRoute } from "../helpers"
import makeMe from "../fixtures/makeMe"

describe("note overview", () => {

  it("should render one note", async () => {
    const note = makeMe.aNote.please()
    renderWithMockRoute(NoteOverview, { props: note })
    await screen.findByText('Desc')
  });

  it("should render note with one child", async () => {
    const note = makeMe.aNote.withAChildNote.please()
    renderWithMockRoute(NoteOverview, { props: note })
    await screen.findByText('Desc')
  });

});

