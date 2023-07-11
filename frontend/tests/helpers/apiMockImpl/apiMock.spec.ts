import { describe, it, expect, beforeEach } from "vitest";
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

  it("should not expect any api call after closd", async () => {
    mockedApi.expectingGet("url");
    mockedApi.close();
    await fetch("url");
    expect(() => mockedApi.assertNoUnexpectedOrMissedCalls()).toThrowError(
      "Expected but missed API calls: url",
    );
  });

  it("should fail if expectation is not met", async () => {
    mockedApi.expectingGet("url");
    expect(() => mockedApi.assertNoUnexpectedOrMissedCalls()).toThrowError(
      "Expected but missed API calls: url",
    );
    mockedApi.assertNoUnexpectedOrMissedCalls(); // should be cleared after assert once
  });

  it("should expect multiple return once", async () => {
    mockedApi.expectingGet("url").andReturnOnce({ v: "1" });
    mockedApi.expectingGet("url").andReturnOnce({ v: "2" });
    expect(await (await fetch("url")).json()).toEqual({ v: "1" });
    expect(await (await fetch("url")).json()).toEqual({ v: "2" });
  });

  it("should fail fast", async () => {
    mockedApi.expectingGet("url");
    mockedApi.expectingGet("url1");
    await expect(fetch("url", { method: "POST" })).rejects.toThrowError(
      "Unexpected API call: 'POST url'",
    );
    expect(() => mockedApi.assertNoUnexpectedOrMissedCalls()).toThrowError(
      /Unexpected API call: 'POST url'/,
    );
  });
});
