import { createTestingPinia } from "@pinia/testing";
import createPiniaStore from "../../src/store/pinia_store";

const pinia = createTestingPinia();

export default createPiniaStore(pinia);