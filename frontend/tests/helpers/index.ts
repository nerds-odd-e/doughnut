import { createTestingPinia, TestingPinia } from "@pinia/testing";
import { defineComponent } from "vue";
import RenderingHelper from "./RenderingHelper";
import setupApiMock from "./apiMockImpl/setupApiMock";
import { ApiMock } from "./ApiMock";

class StoredComponentTestHelper {
  private piniaInstance?: TestingPinia;

  private mockedApi?: ApiMock;

  private get pinia() {
    if (!this.piniaInstance) this.piniaInstance = createTestingPinia();
    return this.piniaInstance;
  }

  get apiMock(): ApiMock {
    if (!this.mockedApi) throw new Error("please call resetWithApiMock first.");
    return this.mockedApi;
  }

  reset() {
    this.piniaInstance = undefined;
    this.mockedApi = undefined;
    return this;
  }

  resetWithApiMock(beforeEach: jest.Lifecycle, afterEach: jest.Lifecycle) {
    beforeEach(() => {
      this.reset();
      this.mockedApi = setupApiMock();
    });
    afterEach(() => {
      this.mockedApi?.assertNoUnexpectedOrMissedCalls();
    });
    return this;
  }

  component(comp: ReturnType<typeof defineComponent>) {
    return new RenderingHelper(comp).withGlobalPlugin(this.pinia);
  }
}

export default new StoredComponentTestHelper();
export { setupApiMock };
