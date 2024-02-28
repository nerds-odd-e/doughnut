import ManagedApi, { ApiStatus } from "@/managedApi/ManagedApi";
import helper from "../helpers";

helper.resetWithApiMock(beforeEach, afterEach);

describe("managdApi", () => {
  const apiStatus: ApiStatus = { states: [], errors: [] };
  const managedApi = new ManagedApi(apiStatus);

  describe("set the loading status", () => {
    it("should set the loading status", async () => {
      let interimStateLength = 0;
      helper.apiMock
        .expectingGet(`/api/user`)
        .andRespondWithAsyncPromiseResolve(() => {
          interimStateLength = apiStatus.states.length;
        });
      await managedApi.restUserController.getUserProfile();
      expect(interimStateLength).toBeGreaterThan(0);
      expect(apiStatus.states.length).toBe(0);
    });

    it("should not set the loading status in silent mode", async () => {
      let interimStateLength = 0;
      helper.apiMock
        .expectingGet(`/api/user`)
        .andRespondWithAsyncPromiseResolve(() => {
          interimStateLength = apiStatus.states.length;
        });
      await managedApi.silent.restUserController.getUserProfile();
      expect(interimStateLength).toBe(0);
    });
  });

  describe("collect error msg", () => {
    beforeEach(() => {
      vitest.useFakeTimers();
      helper.apiMock.expectingGet(`/api/user`).andRespondOnceWith404();
    });

    const callApiAndIgnoreError = async () => {
      try {
        await managedApi.restUserController.getUserProfile();
      } catch (e) {
        // ignore
      }
    };

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
