import { ref } from "vue";
import api from "./api";

export default function (options={initalLoading: false, hasFormError: false}) {
  const loading = ref(options.initalLoading)
  const formErrors = options.hasFormError ? ref({}) : undefined
  const loadingData = { loading, formErrors }
  return {
    get api() { return api(loadingData) },
    ...loadingData
  };
}