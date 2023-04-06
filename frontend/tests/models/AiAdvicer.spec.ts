import AiAdvicer from "@/models/AiAdvicer";
import makeMe from "tests/fixtures/makeMe";

describe("AiAdvicer", () => {
  const note = makeMe.aNote.description("").please();
  const advicer = new AiAdvicer(note.textContent);

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
