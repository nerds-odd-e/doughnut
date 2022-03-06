import useStore from "./pinia_store";
import createStoredApi from  "../managedApi/createStoredApi";

export default function(component) {
  return {
    setup() {
      const piniaStore = useStore()
      return {
        piniaStore,
        storedApiExp(options={}) { return createStoredApi(this, options) }
      }
    },
    ...component,
  }

}