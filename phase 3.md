# Common Git Collaboration & Safe Development Guide

### Purpose

Undrift is developed collaboratively. Multiple contributors may work on different agents, Android components, backend logic, UI, and infrastructure simultaneously.

The purpose of this guide is to prevent one contributor's work from accidentally:

- Overwriting another contributor's work.
- Creating unnecessary merge conflicts.
- Breaking the shared branch.
- Deleting someone else's changes.
- Introducing unreviewed code.
- Making local changes impossible to reproduce.

The core principle is:

> **Your branch is yours. Shared branches are everyone's.**
> 

---

# 1. NEVER WORK DIRECTLY ON MAIN

Do not make feature changes directly on:

```
main
```

or any other protected/shared integration branch.

Create a dedicated branch for every task.

Example:

```bash
git switch main
git pull
git switch -c feature/context-aware-agent
```

Other examples:

```
feature/minimal-intervention-agent
feature/reward-loop-agent
feature/usage-monitoring
feature/notification-system
fix/database-migration
fix/notification-crash
test/agent-evaluation
```

Use descriptive branch names.

---

# 2. ONE TASK = ONE BRANCH

Do not put unrelated work into the same branch.

Bad:

```
feature/my-work

- Agent prompt
- UI redesign
- Database migration
- Random bug fix
- README changes
```

Better:

```
feature/minimal-intervention-agent
```

and separately:

```
fix/notification-permission
```

This keeps changes reviewable and reduces conflicts.

---

# 3. BEFORE STARTING WORK

Always synchronize your local repository before creating or continuing a feature.

Run:

```bash
git status
git fetch origin
```

Then update your base branch:

```bash
git switch main
git pull --ff-only
```

Create or update your feature branch from the latest main.

Example:

```bash
git switch -c feature/my-feature
```

If the branch already exists:

```bash
git switch feature/my-feature
git rebase main
```

Do not blindly use `git pull` if you do not understand what it will merge.

Prefer:

```bash
git pull --ff-only
```

for shared branches.

---

# 4. CHECK YOUR WORKING TREE

Before changing anything:

```bash
git status
```

Understand what files are already modified.

Never assume that every local change belongs to you.

If you see changes you did not make:

DO NOT immediately overwrite, reset, checkout, or delete them.

First determine what they are.

Someone may be working on the same local environment.

---

# 5. DO NOT USE DANGEROUS COMMANDS CASUALLY

Do not run commands such as:

```bash
git reset --hard
git clean -fd
git checkout .
git restore .
git push --force
git push --force-with-lease
```

unless you fully understand what will be affected.

These commands can destroy local work.

If you need to discard your own changes, verify exactly what will be removed first.

---

# 6. COMMIT SMALL, LOGICAL CHANGES

Commit related changes together.

Good:

```
Add intervention decision logic
```

Good:

```
Add reward loop agent evaluation tests
```

Bad:

```
Update everything
```

Bad:

```
changes
```

A useful commit should represent one logical change.

---

# 7. DO NOT MIX OTHER PEOPLE'S WORK INTO YOUR COMMIT

Before committing:

```bash
git status
git diff
```

Review the exact changes.

Then stage only the files relevant to your task.

Prefer:

```bash
git add path/to/file
```

instead of blindly doing:

```bash
git add .
```

when your working tree contains unrelated changes.

Before committing:

```bash
git diff --cached
```

Verify that the staged diff contains only your intended work.

---

# 8. COMMIT FREQUENTLY

Do not work for three days and then create one giant commit.

Prefer:

```
commit 1
Initial agent structure

commit 2
Add intervention decision logic

commit 3
Add cooldown handling

commit 4
Add agent tests
```

Small commits make it much easier to:

- Review changes.
- Identify bugs.
- Revert mistakes.
- Resolve conflicts.
- Understand project history.

---

# 9. PUSH YOUR BRANCH

Push your feature branch:

```bash
git push -u origin feature/my-feature
```

After the first push:

```bash
git push
```

Do not push your feature work directly to `main`.

---

# 10. KEEP YOUR BRANCH UPDATED

Other contributors will continue changing `main`.

Your branch can therefore become outdated.

Periodically:

```bash
git fetch origin
git rebase origin/main
```

This keeps your branch current.

Only rebase your own feature branch.

Do NOT rebase a branch that other people are actively using unless everyone coordinating that branch agrees.

---

# 11. REBASE VS MERGE

For personal feature branches, prefer:

```bash
git fetch origin
git rebase origin/main
```

This keeps history clean.

For shared branches, avoid rewriting history.

Never rewrite the history of:

```
main
```

or another branch that other contributors depend on.

---

# 12. WHEN TWO PEOPLE MODIFY THE SAME FILE

This is normal.

For example:

```
Person A
→ Minimal Intervention Agent

Person B
→ Notification system
```

If both modify:

```
AgentService.kt
```

a merge conflict may occur.

Do NOT solve this by blindly choosing:

```
ours
```

or:

```
theirs
```

First understand both changes.

Ask:

1. What was Person A trying to change?
2. What was Person B trying to change?
3. Can both changes coexist?
4. Does the final code preserve both behaviors?

Then manually resolve the conflict.

---

# 13. CONFLICT RESOLUTION

When Git reports a conflict:

```
<<<<<<< HEAD
your changes
=======
incoming changes
>>>>>>> origin/main
```

Do not simply delete one side.

Determine the intended final implementation.

After resolving:

```bash
git add <resolved-file>
```

Then continue the operation:

For rebase:

```bash
git rebase --continue
```

For merge:

```bash
git commit
```

Then run the relevant tests again.

A conflict resolution is a code change and must be tested.

---

# 14. IF YOU ARE UNSURE, STOP

If you encounter a complicated merge conflict:

Do NOT:

- Randomly delete code.
- Choose one side blindly.
- Force-push.
- Reset the branch.
- Ask Git to automatically resolve everything.

Instead:

```
STOP
↓
Inspect the conflict
↓
Understand both changes
↓
Coordinate with the other contributor
↓
Resolve intentionally
↓
Run tests
```

Preserving another person's work is more important than resolving the conflict quickly.

---

# 15. COORDINATE OWNERSHIP OF FILES

Whenever practical, divide work by components.

Example:

```
Person A
Context-Aware Agent
Agent context logic
Context tests

Person B
Minimal-Intervention Agent
Intervention logic
Intervention tests

Person C
Reward Loop Agent
Reward logic
Reward tests
```

If multiple people need to modify the same shared file, communicate before doing so.

This is especially important for:

- Gradle files
- AndroidManifest.xml
- Shared data models
- Navigation
- Dependency injection
- Database schemas
- Common services
- Shared configuration
- API interfaces

---

# 16. DO NOT MODIFY SHARED INFRASTRUCTURE WITHOUT WARNING

Before changing something used by the whole application, inform the team.

Examples:

```
build.gradle
settings.gradle
libs.versions.toml
AndroidManifest.xml
database schema
shared models
API contracts
authentication
common utilities
```

These files have a large blast radius.

A small change can break everyone's branches.

---

# 17. AI PROMPTS ARE CODE

For Undrift, prompts are part of application behavior.

Therefore treat:

```
system prompts
agent instructions
JSON schemas
agent configuration
evaluation datasets
```

as source code.

Changes to prompts should:

- Be committed.
- Be reviewed.
- Have tests/evaluation where possible.
- Not silently overwrite another person's prompt.
- Include a meaningful commit message.

Do not modify another person's agent prompt without coordination.

---

# 18. TEST BEFORE PUSHING

Before pushing significant changes:

```bash
./gradlew test
./gradlew lint
./gradlew assembleDebug
```

If applicable:

```bash
./gradlew connectedAndroidTest
```

Do not knowingly push broken code and expect someone else to fix it.

If a failure is pre-existing, document it.

---

# 19. PULL REQUESTS

Use Pull Requests for merging feature branches into `main`.

A PR should explain:

```
What changed?
Why was it changed?
How was it tested?
Any known limitations?
```

Example:

```
## Changes

Implemented Minimal-Intervention Agent decision logic.

## Includes

- Intervention threshold
- Cooldown handling
- Context confidence handling
- Intervention levels
- Structured output

## Testing

- Unit tests
- Edge cases
- Gradle build
- Lint

## Known limitations

LLM behavior currently evaluated using deterministic test scenarios.
```

---

# 20. REVIEW BEFORE MERGING

Do not merge a PR merely because:

```
"it looks fine"
```

Review:

- Functional changes.
- Potential regressions.
- Error handling.
- Tests.
- AI behavior.
- Android lifecycle implications.
- Security/privacy implications.
- Unnecessary dependencies.
- Performance implications.

The author should explain unusual implementation decisions.

---

# 21. NEVER FORCE-PUSH TO MAIN

This rule is absolute.

Never:

```bash
git push --force origin main
```

Never rewrite shared branch history.

If a shared branch has a problem, fix it with a normal commit or coordinate with the team.

---

# 22. PROTECT MAIN

The repository should ideally configure `main` with:

- Pull request requirement.
- At least one review.
- Passing automated tests.
- Passing build.
- No direct pushes.
- No force pushes.

The exact Git hosting configuration may vary.

The principle remains:

> Main should always represent a reasonably working version of the application.
> 

---

# 23. IF YOUR LOCAL WORK IS NOT READY

Do not create a broken commit just because you need to switch branches.

Use:

```bash
git stash
```

when appropriate.

Then:

```bash
git stash pop
```

to restore it.

However, prefer commits on your own feature branch when the work represents a meaningful checkpoint.

Do not use stash as a long-term storage mechanism.

---

# 24. IF YOU ACCIDENTALLY CHANGED SOMEONE ELSE'S WORK

STOP.

Do not immediately reset the repository.

First determine whether the changes are:

- Uncommitted local work.
- Committed work.
- Changes from another branch.
- Changes from a merge/rebase.

Preserve the current state before attempting recovery.

When necessary, create a safety branch:

```bash
git switch -c recovery/my-current-state
```

Then investigate.

Git is powerful, but careless recovery attempts can destroy the original state.

---

# 25. BEFORE MERGING

The contributor responsible for the PR should:

```bash
git fetch origin
git rebase origin/main
```

if the branch is not shared and the team's workflow uses rebase.

Then run:

```bash
./gradlew clean
./gradlew test
./gradlew lint
./gradlew assembleDebug
```

If instrumentation tests are available:

```bash
./gradlew connectedAndroidTest
```

Push the updated branch.

Then wait for CI to pass.

Only then merge.

---

# 26. AFTER MERGING

Once your PR is merged:

```bash
git switch main
git pull --ff-only
```

You can then delete your local feature branch:

```bash
git branch -d feature/my-feature
```

and, if appropriate, delete the remote feature branch through the repository hosting platform.

---

# 27. KEEP COMMITS REVERTABLE

Avoid enormous commits that combine unrelated changes.

A good commit should ideally be independently understandable.

For example:

```
Add Minimal-Intervention Agent
```

is better than:

```
Add agent + redesign UI + change database + update Gradle + fix notifications
```

If something breaks, the smaller commit can be identified and reverted more safely.

---

# 28. DO NOT COMMIT GENERATED OR MACHINE-SPECIFIC FILES

Follow the repository's `.gitignore`.

Do not commit things such as:

```
build/
.gradle/
local.properties
IDE-specific local configuration
generated artifacts
temporary files
logs
secrets
API keys
```

Never commit:

- Passwords
- API keys
- Access tokens
- Private credentials
- Local secrets

If a secret is accidentally committed, report it immediately and rotate/revoke it.

Removing it from the latest commit does not necessarily remove it from Git history.

---

# 29. KEEP CHANGES FOCUSED

If you notice an unrelated bug while working:

Do not automatically mix its fix into your current feature.

Instead:

1. Record the bug.
2. Create a separate issue/task.
3. Create a separate branch if the fix is needed immediately.
4. Keep the current feature focused.

Exception:

If the bug directly prevents your feature from functioning, coordinate with the team and clearly document the related fix.

---

# 30. COMMUNICATION RULE

Before making changes that affect another person's area, communicate.

Example:

```
"I'm modifying AgentService.kt because the intervention agent needs a new context field. This will affect the Context-Aware Agent interface too."
```

This takes 20 seconds and can prevent an hour of conflict resolution.

---

# 31. SAFE COLLABORATION WORKFLOW

The recommended workflow for every contributor is:

```
1. Pull latest main
        ↓
2. Create personal feature branch
        ↓
3. Work only on your assigned task
        ↓
4. Test locally
        ↓
5. Commit logical changes
        ↓
6. Push feature branch
        ↓
7. Open Pull Request
        ↓
8. CI runs
        ↓
9. Code review
        ↓
10. Resolve conflicts if required
        ↓
11. Re-run tests
        ↓
12. Merge
        ↓
13. Update local main
```

---

# 32. GOLDEN RULES

Every contributor should remember these rules:

### Rule 1

Never work directly on `main`.

### Rule 2

Never force-push shared branches.

### Rule 3

Never overwrite someone else's changes without understanding them.

### Rule 4

Never blindly resolve a conflict using "ours" or "theirs".

### Rule 5

Never commit unrelated work.

### Rule 6

Always inspect `git diff` before committing.

### Rule 7

Always test before opening/merging a PR.

### Rule 8

Treat prompts, schemas, and agent configuration as code.

### Rule 9

Communicate before modifying shared infrastructure.

### Rule 10

If you are unsure whether a Git command could destroy work, STOP and ask before running it.

---

# FINAL PRINCIPLE

Git is not just a place to upload code.

It is the coordination mechanism between every contributor.

The goal is not:

"How quickly can I get my code into main?"

The goal is:

"How can I integrate my work without breaking or overwriting anyone else's work?"

When in doubt:

```
FETCH
↓
INSPECT
↓
COMMUNICATE
↓
CHANGE
↓
TEST
↓
COMMIT
↓
PUSH
↓
REVIEW
↓
MERGE
```

Never skip the inspection and communication steps when working around shared code.