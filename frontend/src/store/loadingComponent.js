import { ref } from "vue";
import api from "../managedApi/api";

function createComponent(initalLoading, component) {
  return {
    setup() {
      const loading = ref(initalLoading)
      return {
        apiExp() { return api(this) },
        loading,
      }
    },
    ...component,
  }

}

export default function(initalLoading, component) {
  if (component === undefined) {
    return createComponent(false, component);
  }
  return createComponent(initalLoading, component);
}
