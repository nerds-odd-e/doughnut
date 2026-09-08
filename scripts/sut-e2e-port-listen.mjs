import net from 'node:net'

export function closeListeningServer(server) {
  return new Promise((resolve) => {
    server.close(() => resolve())
  })
}

export function listenEphemeralPort(createServer = () => net.createServer()) {
  return new Promise((resolve, reject) => {
    const server = createServer()
    server.once('error', reject)
    server.listen(0, '127.0.0.1', () => {
      const address = server.address()
      if (!address || typeof address === 'string') {
        server.close(() =>
          reject(new Error('failed to allocate an ephemeral TCP port'))
        )
        return
      }
      resolve({ server, port: address.port })
    })
  })
}
