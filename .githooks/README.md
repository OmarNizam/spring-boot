# Git hooks

Tracked hooks for this repo. Git does not use them automatically — activate once
per clone:

```sh
git config core.hooksPath .githooks
```

## pre-push

Runs the Claude Code `code-reviewer` subagent (`.claude/agents/code-reviewer.md`)
against the commits being pushed and prints the review. It **blocks the push only
when the review reports a 🔴 blocker** (`REVIEW_RESULT: BLOCK`).

Fails open: if the `claude` CLI is missing or the review errors out, the push is
allowed. Requires the `claude` CLI on `PATH` (or at `~/.local/bin/claude`).

Bypass:

```sh
SKIP_CODE_REVIEW=1 git push   # skip just this review
git push --no-verify          # skip all hooks
```
