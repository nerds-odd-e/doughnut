import { defineComponent } from "vue";
import { enableAutoUnmount } from "@vue/test-utils";
import RenderingHelper from "./RenderingHelper";
import setupApiMock from "./apiMockImpl/setupApiMock";
import { ApiMock } from "./ApiMock";
import matchByText from "./matchByText";

class StoredComponentTestHelper {
  private mockedApi?: ApiMock;

  get apiMock(): ApiMock {
    if (!this.mockedApi) throw new Error("please call resetWithApiMock first.");
    return this.mockedApi;
  }

  reset() {
    this.mockedApi?.close();
    this.mockedApi = undefined;
    return this;
  }

  resetWithApiMock(beforeEach, afterEach) {
    enableAutoUnmount(afterEach);
    beforeEach(() => {
      this.reset();
      this.mockedApi = setupApiMock();
    });
    afterEach(() => {
      this.mockedApi?.assertNoUnexpectedOrMissedCalls();
    });
    return this;
  }

  // eslint-disable-next-line class-methods-use-this
  component(comp: ReturnType<typeof defineComponent>) {
    return new RenderingHelper(comp);
  }
}

export default new StoredComponentTestHelper();
export { setupApiMock, matchByText };
