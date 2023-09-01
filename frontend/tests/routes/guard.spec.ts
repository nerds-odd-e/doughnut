import { describe, it, expect, beforeEach, vi } from "vitest";
import routerScopeGuard from "../../src/routes/relative_routes";

describe("router guards", () => {
  let guard;
  let next;

  beforeEach(async () => {
    guard = routerScopeGuard("repeat");
    next = vi.fn();
  });
  it("when in repeat, go to nested noteShow", async () => {
    await guard({ name: "noteShow", params: { noteId: 3 } }, next);
    expect(next).toHaveBeenCalledWith({
      name: "repeat-noteShow",
      params: { noteId: 3 },
    });
  });

  it("when in repeat, and going to already nested route", async () => {
    await guard({ name: "repeat-noteShow", params: { noteId: 3 } }, next);
    expect(next).toHaveBeenCalledWith();
  });

  it("when in repeat, and going to a route that doesnot have nested route", async () => {
    await guard({ name: "initial", params: { noteId: 3 } }, next);
    expect(next).toHaveBeenCalledWith();
  });

  it("when in repeat-quiz, and going to reviews", async () => {
    await guard({ name: "reviews" }, next);
    expect(next).toHaveBeenCalledTimes(1);
  });
});
