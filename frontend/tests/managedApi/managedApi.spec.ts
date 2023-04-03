import ManagedApi from "@/managedApi/ManagedApi";
import helper from "../helpers";

helper.resetWithApiMock(beforeEach, afterEach);

describe("managdApi", () => {
  const managedApi = new ManagedApi();

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
      expect(ManagedApi.statusWrap.apiStatus.lastErrorMessage).toEqual(
        "got 404"
      );
    });
  });
});
