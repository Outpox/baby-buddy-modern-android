#!/usr/bin/env bash
# Fails if any given file contains a word from tools/hooks/blocklist.local (gitignored):
# names, the server's address and other personal details that must stay out of git.
# Matching is case-insensitive and on whole words. Without a blocklist (e.g. in CI) it passes.
set -u
blocklist="$(git rev-parse --show-toplevel)/tools/hooks/blocklist.local"
[ -f "$blocklist" ] || exit 0
words=$(grep -v -e '^[[:space:]]*#' -e '^[[:space:]]*$' "$blocklist" | tr -d '\r')
[ -n "$words" ] || exit 0

status=0
for f in "$@"; do
  [ "$f" = "tools/hooks/blocklist.local" ] && continue
  # -I skips binary files (screenshots can't be checked this way; look at them yourself)
  hits=$(grep -I -n -i -w -F -f <(printf '%s\n' "$words") -- "$f" | cut -c1-160)
  if [ -n "$hits" ]; then
    printf '%s: personal word from tools/hooks/blocklist.local:\n%s\n' "$f" "$(printf '%s\n' "$hits" | sed 's/^/    /')" >&2
    status=1
  fi
done
exit $status
