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

  component(comp: DefineComponent) {
    return new RenderingHelper(comp).withGlobal({plugins: [this.pinia]})
  }

}

export { StoredComponentTestHelper };
