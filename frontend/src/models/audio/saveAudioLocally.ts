export const saveAudioLocally = (audio: Blob) => {
  const url = URL.createObjectURL(audio)
  const a = document.createElement("a")
  a.href = url
  a.download = "recorded_audio.wav"
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}
