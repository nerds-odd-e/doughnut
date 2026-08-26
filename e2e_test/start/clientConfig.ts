import { client } from '@generated/donut-backend-api/client.gen'

client.setConfig({
  baseUrl: '',
  throwOnError: true,
  responseStyle: 'data',
  credentials: 'include',
})
