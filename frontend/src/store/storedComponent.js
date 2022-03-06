import useStore from '../store/pinia_store';

export default function(component) {
  return {
    setup() {
      const piniaStore = useStore()
      return { piniaStore }
    },
    ...component,
  }

}