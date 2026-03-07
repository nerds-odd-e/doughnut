import { client } from '@generated/doughnut-backend-api/client.gen'

client.setConfig({
  baseUrl: '',
  throwOnError: true,
  responseStyle: 'data',
  credentials: 'include',
})
