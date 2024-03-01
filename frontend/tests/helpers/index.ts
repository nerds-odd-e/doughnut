import { defineComponent } from "vue";
import ManagedApi from "@/managedApi/ManagedApi";
import RenderingHelper from "./RenderingHelper";
import setupApiMock from "./apiMockImpl/setupApiMock";
import matchByText from "./matchByText";

class StoredComponentTestHelper {
  public managedApi = new ManagedApi({ states: [], errors: [] });

  // eslint-disable-next-line class-methods-use-this
  component(comp: ReturnType<typeof defineComponent>) {
    return new RenderingHelper(comp, this.managedApi);
  }
}

export default new StoredComponentTestHelper();
export { setupApiMock, matchByText };
