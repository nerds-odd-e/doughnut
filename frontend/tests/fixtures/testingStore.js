import { setActivePinia } from "pinia";
import { createTestingPinia } from "@pinia/testing";
import createPiniaStore from "../../src/store/createPiniaStore";

setActivePinia(createTestingPinia());
export default createPiniaStore();
