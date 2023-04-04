import ManagedApi, { ApiStatus } from "@/managedApi/ManagedApi";
import helper from "../helpers";

helper.resetWithApiMock(beforeEach, afterEach);

describe("managdApi", () => {
  const apiStatus: ApiStatus = { states: [], errors: [] };
  const managedApi = new ManagedApi(apiStatus);

  beforeEach(() => {
    helper.apiMock.expectingGet(`/api/call`).andRespondOnceWith404();
  });

  describe("rendering a note realm", () => {
    it("should render note with one child", async () => {
      try {
        await managedApi.restGet(`/api/call`);
      } catch (e) {
        // ignore
      }
      expect(apiStatus.errors).toHaveLength(1);
    });
  });
});
