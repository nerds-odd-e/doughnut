#!/bin/bash
TIMER_FILE="/tmp/cursor-doughnut-planning-timer"

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
  cat <<'EOF'
{"additional_context": "You have been working for over 10 minutes. Stop implementation and use the phased-planning skill: summarize what you have learned, stash your changes with git stash, decompose the remaining work into phases in ongoing/<name>.md, then stop and let the developer decide next steps."}
EOF
  exit 0
fi

echo '{}'
exit 0
