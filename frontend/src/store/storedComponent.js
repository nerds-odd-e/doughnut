import useStore from "./pinia_store";

export default function(component) {
  return {
    setup() {
      const piniaStore = useStore()
      return { piniaStore }
    },
    ...component,
  }

}