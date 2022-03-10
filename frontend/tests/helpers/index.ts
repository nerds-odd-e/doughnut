import fetchMock from "jest-fetch-mock";
import { createTestingPinia, TestingPinia } from "@pinia/testing";
import { DefineComponent } from "vue";
import createPiniaStore from '../../src/store/createPiniaStore';
import RenderingHelper  from "./RenderingHelper";
import ApiMock  from "./ApiMock";

type PiniaStore = ReturnType<typeof createPiniaStore>

class StoredComponentTestHelper {
  private piniaInstance?: TestingPinia

  private piniaStore?: PiniaStore

  private mockedApi?: ApiMock

  private get pinia() {
    return this.piniaInstance || (this.piniaInstance = createTestingPinia())
  }

  get store(): PiniaStore {
    return this.piniaStore || (this.piniaStore = createPiniaStore(this.pinia))
  }

  get apiMock(): ApiMock {
    return this.mockedApi || (this.mockedApi = (()=>{
      fetchMock.resetMocks();
      return new ApiMock(fetchMock);
    })())
  }

  reset() {
    this.piniaInstance = undefined
    this.piniaStore = undefined
    this.mockedApi = undefined
    return this
  }

  component(comp: DefineComponent) {
    return new RenderingHelper(comp).withGlobalPlugin(this.pinia)
  }
}

export default new StoredComponentTestHelper();
