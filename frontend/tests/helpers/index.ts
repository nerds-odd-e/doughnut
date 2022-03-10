import fetchMock from "jest-fetch-mock";
import { createTestingPinia, TestingPinia } from "@pinia/testing";
import { DefineComponent } from "vue";
import createPiniaStore from '../../src/store/createPiniaStore';
import RenderingHelper  from "./RenderingHelper";

type PiniaStore = ReturnType<typeof createPiniaStore>

class StoredComponentTestHelper {
  private piniaInstance?: TestingPinia

  private piniaStore?: PiniaStore

  private get pinia() {
    return this.piniaInstance || (this.piniaInstance = createTestingPinia())
  }

  get store(): PiniaStore {
    return this.piniaStore || (this.piniaStore = createPiniaStore(this.pinia))
  }

  reset() {
    this.piniaInstance = undefined
    this.piniaStore = undefined
    return this
  }

  useFetchMock(onceDefault: any | undefined = undefined) {
    fetchMock.resetMocks();
    onceDefault && fetchMock.mockResponseOnce(JSON.stringify(onceDefault));
  }

  component(comp: DefineComponent) {
    return new RenderingHelper(comp).withGlobal({plugins: [this.pinia]})
  }

}

export default new StoredComponentTestHelper();
