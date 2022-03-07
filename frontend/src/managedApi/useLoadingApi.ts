import { ref } from "vue";
import api from "./api";

export default function (options={initalLoading: false, hasFormError: false}) {
  const loading = ref(options.initalLoading)
  const formErrors = options.hasFormError ? ref({}) : null
  return {
    apiExp() { return api(this) },
    loading,
    formErrors,
  };
}