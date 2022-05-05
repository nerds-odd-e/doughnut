import { createTestingPinia, TestingPinia } from "@pinia/testing";
import { DefineComponent } from "vue";
import createPiniaStore from "../../src/store/createPiniaStore";
import RenderingHelper from "./RenderingHelper";
import setupApiMock from "./apiMockImpl/setupApiMock";

type PiniaStore = ReturnType<typeof createPiniaStore>;

class StoredComponentTestHelper {
  private piniaInstance?: TestingPinia;

  private piniaStore?: PiniaStore;

  private mockedApi?: ReturnType<typeof setupApiMock>;

  private mockedApiTeardown?: () => void;

  private get pinia() {
    if (!this.piniaInstance) this.piniaInstance = createTestingPinia();
    return this.piniaInstance;
  }

  get store(): PiniaStore {
    if (!this.piniaStore) this.piniaStore = createPiniaStore(this.pinia);
    return this.piniaStore;
  }

  get apiMock() {
    if (!this.mockedApi) throw new Error("please call resetWithApiMock first.");
    return this.mockedApi;
  }

  reset() {
    this.piniaInstance = undefined;
    this.piniaStore = undefined;
    this.mockedApi = undefined;
    return this;
  }

  resetWithApiMock(beforeEach: jest.Lifecycle, afterEach: jest.Lifecycle) {
    beforeEach(() => {
      this.reset();
      const mock = setupApiMock();
      this.mockedApi = mock.mockedApi;
      this.mockedApiTeardown = mock.teardown;
    });
    afterEach(() => {
      if (this.mockedApiTeardown) this.mockedApiTeardown();
    });
    return this;
  }

  component(comp: DefineComponent) {
    return new RenderingHelper(comp).withGlobalPlugin(this.pinia);
  }
}

export default new StoredComponentTestHelper();
