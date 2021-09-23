/**
 * @jest-environment jsdom
 */
import { render, screen } from "@testing-library/vue";
import NoteShow from "@/components/notes/NoteShow.vue";
import makeMe from "../fixtures/makeMe";

describe("new/updated pink banner", () => {
  beforeAll(() => {
    Date.now = jest.fn(() => new Date(Date.UTC(2017, 1, 14)).valueOf());
  });

  test.each([
    [new Date(Date.UTC(2017, 1, 15)), "rgb(208, 237, 23)"],
    [new Date(Date.UTC(2017, 1, 13)), "rgb(189, 209, 64)"],
    [new Date(Date.UTC(2017, 1, 12)), "rgb(181, 197, 82)"],
    [new Date(Date.UTC(2016, 1, 12)), "rgb(150, 150, 150)"],
  ])(
    "should show fresher color if recently updated",
    (updatedAt, expectedColor) => {
      const note = makeMe.aNote.updatedAt(updatedAt).please();
      render(NoteShow, { props: note });

      expect(screen.getByRole("title").parentNode.parentNode).toHaveStyle(
        `background-color: ${expectedColor};`
      );
    }
  );
});
