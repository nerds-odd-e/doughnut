import { ref } from "vue"

const featureToggle = ref(false)

export function useFeatureToggle() {
  const setFeatureToggle = (enabled: boolean) => {
    featureToggle.value = enabled
  }

  return {
    featureToggle,
    setFeatureToggle,
  }
}
