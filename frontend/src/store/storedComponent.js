import useStore from "./pinia_store";
import createStoredApi from  "../managedApi/createStoredApi";

export default function(component) {
  return {
    ...component,
    setup() {
      const piniaStore = useStore();
      const additional = component.setup !== undefined ? component.setup() : {};
      return {
        ...additional,
        piniaStore,
        storedApi(options={}) { return createStoredApi(this, options) }
      }
    },
  }

}