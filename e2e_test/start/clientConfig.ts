import { client } from '@generated/backend/client.gen'

client.setConfig({
  baseUrl: '',
  throwOnError: true,
  responseStyle: 'data',
  credentials: 'include',
})
