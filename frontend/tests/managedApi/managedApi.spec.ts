import ManagedApi, { ApiStatus } from "@/managedApi/ManagedApi";
import helper from "../helpers";

helper.resetWithApiMock(beforeEach, afterEach);

describe("managdApi", () => {
  const apiStatus: ApiStatus = { states: [], errors: [] };
  const managedApi = new ManagedApi(apiStatus);
  const callApiAndIgnoreError = async () => {
    try {
      await managedApi.restGet(`/api/call`);
    } catch (e) {
      // ignore
    }
  };

  beforeEach(() => {
    vitest.useFakeTimers();
    helper.apiMock.expectingGet(`/api/call`).andRespondOnceWith404();
  });

  describe("rendering a note realm", () => {
    it("should render note with one child", async () => {
      await callApiAndIgnoreError();
      expect(apiStatus.errors).toHaveLength(1);
    });

    it("disappear in 2 seconds", async () => {
      await callApiAndIgnoreError();
      vitest.advanceTimersByTime(2000);
      expect(apiStatus.errors).toHaveLength(0);
    });
  });
});
