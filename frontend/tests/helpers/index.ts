import ManagedApi from "@/managedApi/ManagedApi"
import RenderingHelper from "./RenderingHelper"
import matchByText from "./matchByText"

class StoredComponentTestHelper {
  public managedApi = new ManagedApi({ states: [], errors: [] })

  component<T>(comp: T) {
    return new RenderingHelper(comp, this.managedApi)
  }
}

export default new StoredComponentTestHelper()
export { matchByText }
