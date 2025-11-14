async function createEventSourceWithBody(
  url: string,
  body: unknown,
  onMessage: (event: string, data: string) => void,
  onError?: (error: unknown) => void
) {
  try {
    const response = await fetch(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Accept: "text/event-stream",
      },
      body: JSON.stringify(body),
    })

    if (!response.ok) {
      // Handle HTTP errors before SSE stream starts
      // This catches errors that happen before the SSE connection is established
      const error = new Error(`HTTP error! status: ${response.status}`)
      if (onError) {
        onError(error)
      }
      return
    }

    const reader = response.body?.getReader()
    const decoder = new TextDecoder("utf-8")

    if (!reader) {
      throw new Error("Failed to get reader from response body")
    }

    let buffer = ""

    const read = () => {
      reader
        .read()
        .then(({ done, value }) => {
          if (done) {
            return
          }

          buffer += decoder.decode(value, { stream: true })
          processBuffer()

          read()
        })
        .catch((error) => {
          if (onError) {
            onError(error)
          }
        })
    }

    const processBuffer = () => {
      let pos
      // eslint-disable-next-line no-cond-assign
      while ((pos = buffer.indexOf("\n\n")) !== -1) {
        const chunk = buffer.slice(0, pos)
        buffer = buffer.slice(pos + 2)
        processChunk(chunk)
      }
    }

    const processChunk = (chunk: string) => {
      const lines = chunk.split("\n")
      let event = ""
      let data = ""

      lines.forEach((line) => {
        if (line.startsWith("event:")) {
          event = line.substring(6).trim()
        } else if (line.startsWith("data:")) {
          data += `${line.substring(5).trim()}\n`
        }
      })

      if (data) {
        // Trim trailing newline from data
        data = data.trim()
        onMessage(event, data)
      }
    }

    read()
  } catch (error) {
    if (onError) {
      onError(error)
    }
  }
}

export default createEventSourceWithBody
