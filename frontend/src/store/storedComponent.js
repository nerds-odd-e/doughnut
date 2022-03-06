import useStore from "./pinia_store";
import storedApi from  "../managedApi/storedApi";

export default function(component) {
  return {
    setup() {
      const piniaStore = useStore()
      return {
        piniaStore,
        storedApiExp(options={}) { return storedApi(this, options) }
      }
    },
    ...component,
  }

}