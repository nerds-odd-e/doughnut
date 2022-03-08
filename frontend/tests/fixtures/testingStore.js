import { setActivePinia, createPinia } from 'pinia';
import { createTestingPinia } from '@pinia/testing';
import createPiniaStore from '../../src/store/pinia_store';

setActivePinia(createPinia());
// const pinia = createTestingPinia();

// export default createPiniaStore(pinia);
export default createPiniaStore();
