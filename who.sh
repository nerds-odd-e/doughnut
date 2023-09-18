yarn sound https://github.com/nerds-odd-e/doughnut/actions 2>&1 | 
tee /dev/tty | 
while IFS= read -r line; do
  if [[ "$line" == *"build"* ]]; then
    afplay docs/who.mp3 &
  fi
done
