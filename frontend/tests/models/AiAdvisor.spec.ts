import AiAdvisor from "@/models/AiAdvisor";
import makeMe from "tests/fixtures/makeMe";

describe("AiAdvisor", () => {
  const note = makeMe.aNote.description("").please();
  const advicer = new AiAdvisor(note.textContent);

  it("in case the description is null", () => {
    note.textContent.description = null as unknown as string;
    expect(advicer.prompt()).toBe(
      "Complete the description for the following note:\n" +
        "title: Note1.1.1\n" +
        "description:\n" +
        "---\n"
    );
  });

  it("use the full text", () => {
    expect(advicer.processResult("abc")).toBe("abc");
  });

  it("removes the prompt if any", () => {
    expect(advicer.processResult("abc---\ndef")).toBe("def");
  });
  it("removes only the prompt", () => {
    expect(advicer.processResult("abc---\ndef---\ng")).toBe("def---\ng");
  });
});
