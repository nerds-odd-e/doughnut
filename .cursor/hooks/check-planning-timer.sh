#!/bin/bash
TIMER_FILE="/tmp/cursor-doughnut-planning-timer"
RETRO_MARKER="/tmp/cursor-doughnut-retro-fired"

cat > /dev/null

if [ ! -f "$TIMER_FILE" ]; then
  date +%s > "$TIMER_FILE"
  echo '{}'
  exit 0
fi

start_time=$(cat "$TIMER_FILE")
current_time=$(date +%s)
elapsed=$((current_time - start_time))

if [ "$elapsed" -gt 600 ]; then
  date +%s > "$TIMER_FILE"
  rm -f "$RETRO_MARKER"
  cat <<'EOF'
{"additional_context": "You have been working for over 10 minutes. STOP implementation now. Review your conversation history: what files did you search, what confused you, what took multiple attempts? Then use the phased-planning skill: summarize what you learned and what blocked you, stash changes with git stash (unless mid-GSD execute with task commits that must reach SUMMARY — then stop and report without stash), decompose remaining work into Behavior/Structure phases under .planning/quick/NNN-slug/ (or .planning/phases/ if on the roadmap), and report to the developer."}
EOF
  exit 0
fi

if [ "$elapsed" -gt 300 ] && [ ! -f "$RETRO_MARKER" ]; then
  touch "$RETRO_MARKER"
  cat <<'EOF'
{"additional_context": "You have been working for over 5 minutes. Pause briefly and use the codebase-retrospective skill. Review your conversation history: where did you lose time? Was it confusing names, code scattered across files, missing docs, or slow tests? Follow the skill to fix simple friction or propose improvements, then continue your work."}
EOF
  exit 0
fi

echo '{}'
exit 0
