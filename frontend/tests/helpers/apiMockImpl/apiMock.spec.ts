import setupApiMock from "./setupApiMock";

describe("apiMock", () => {
  let mockedApi: ReturnType<typeof setupApiMock>;

  beforeEach(() => {
    mockedApi = setupApiMock();
  });

  it("should expect an api call", async () => {
    mockedApi.expectingGet("url");
    await fetch("url");
    mockedApi.assertNoUnexpectedOrMissedCalls();
  });

  it("should fail if expectation is not met", async () => {
    mockedApi.expecting("url");
    expect(() => mockedApi.assertNoUnexpectedOrMissedCalls()).toThrowError(
      "Expected but missed API calls: url"
    );
    mockedApi.assertNoUnexpectedOrMissedCalls(); // should be cleared after assert once
  });

  xit("should expect an api call only once", async () => {
    mockedApi.expectingGet("url").andReturnOnce({ v: "token" });
    expect(await (await fetch("url")).json()).toEqual({ v: "token" });
    expect(await (await fetch("url")).json()).not.toEqual({ v: "token" });
    expect(() => mockedApi.assertNoUnexpectedOrMissedCalls()).toThrowError(
      "Unexpected API calls: url"
    );
  });

  xit("should expect multiple return once", async () => {
    mockedApi.expectingGet("url").andReturnOnce({ v: "1" });
    mockedApi.expectingGet("url").andReturnOnce({ v: "2" });
    expect(await (await fetch("url")).json()).toEqual({ v: "1" });
    expect(await (await fetch("url")).json()).toEqual({ v: "2" });
  });

  xit("should fail fast", async () => {
    mockedApi.expectingGet("url");
    mockedApi.expectingGet("url1");
    await expect(fetch("url", { method: "POST" })).rejects.toThrowError(
      "Expected GET but got POST"
    );
    mockedApi.assertNoUnexpectedOrMissedCalls();
  });
});
