import { AiAudioController } from "@generated/donut-backend-api/sdk.gen"
import type { AudioChunk } from "@/models/audio/audioProcessingScheduler"

/** Transcribe one recorder chunk; throws when the request fails. */
export async function audioChunkToText(chunk: AudioChunk): Promise<{
  segmentTexts: string[]
  endTimestamp: string | undefined
}> {
  const { data: response, error } = await AiAudioController.audioToText({
    body: {
      uploadAudioFile: chunk.data,
      midSpeech: chunk.isMidSpeech,
    },
  })
  if (error || !response) {
    throw new Error("Failed to process audio")
  }
  return {
    segmentTexts: response.segmentTexts ?? [],
    endTimestamp: response.endTimestamp,
  }
}
