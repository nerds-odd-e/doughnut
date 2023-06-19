import AiAdvisor from "@/models/AiAdvisor";
import makeMe from "tests/fixtures/makeMe";

describe("AiAdvisor", () => {
  const note = makeMe.aNote.description("").please();
  const advisor = new AiAdvisor(note.textContent);

  it("in case the description is null", () => {
    note.textContent.description = null as unknown as string;
    expect(advisor.promptWithContext()).toBe(
      "Describe the note with title: Note1.1.1"
    );
  });
});
