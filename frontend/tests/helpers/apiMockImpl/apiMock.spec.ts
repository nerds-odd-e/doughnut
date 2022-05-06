import setupApiMock from "./setupApiMock";

describe("apiMock", () => {
  it("should fail if expectation is not met", async () => {
    const mockedApi = setupApiMock();
    mockedApi.expecting("url");
    expect(() => mockedApi.assertNoUnexpectedOrMissedCalls()).toThrowError(
      "Expected but missed API calls: url"
    );
    mockedApi.assertNoUnexpectedOrMissedCalls(); // should be cleared after assert once
  });

  it("should expect an api call", async () => {
    const mockedApi = setupApiMock();
    mockedApi.expecting("url");
    fetch("url");
    mockedApi.assertNoUnexpectedOrMissedCalls();
  });
});
