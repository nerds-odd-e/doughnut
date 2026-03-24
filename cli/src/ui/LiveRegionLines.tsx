import { Box, Text } from 'ink'

export function LiveRegionLines({ lines }: { lines: readonly string[] }) {
  return (
    <Box flexDirection="column">
      {lines.map((line, i) => (
        <Text key={i}>{line}</Text>
      ))}
    </Box>
  )
}
