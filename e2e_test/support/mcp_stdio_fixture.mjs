import { createInterface } from 'node:readline'

const rl = createInterface({ input: process.stdin })

rl.on('line', (line) => {
  const message = JSON.parse(line)
  if (message.method !== 'initialize') return
  process.stdout.write(
    `${JSON.stringify({
      jsonrpc: '2.0',
      id: message.id,
      result: {
        protocolVersion: message.params.protocolVersion,
        capabilities: {},
        serverInfo: { name: 'mcp-stdio-fixture', version: '0.0.0' },
      },
    })}\n`
  )
})

rl.on('close', () => {
  process.exit(0)
})
