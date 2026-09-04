# Common Testing, Verification & Quality Assurance Instructions

### Purpose

Every contributor working on Undrift must verify that their changes do not break the application before considering their work complete.

Testing must be performed through the command line and automated tooling wherever possible.

Android Studio is NOT required for the standard verification workflow.

The goal is to verify:

1. The project builds successfully.
2. Existing functionality still works.
3. New functionality works as intended.
4. Tests pass.
5. Static analysis passes.
6. No obvious runtime or integration failures were introduced.
7. The application can be installed and launched in an Android environment.
8. Changes do not break unrelated components.

---

# 1. GENERAL RULE

Never consider a feature complete merely because the code compiles locally.

Every change must go through the verification pipeline.

Minimum verification:

```
Code change
    ↓
Compile
    ↓
Unit tests
    ↓
Static analysis
    ↓
Build APK
    ↓
Install/run verification
    ↓
Integration/UI tests where applicable
    ↓
Final regression check
```

If any stage fails, investigate and fix the issue before declaring the change complete.

Do not hide, disable, skip, or weaken an existing test merely to make the pipeline pass.

---

# 2. ANDROID STUDIO IS NOT REQUIRED

The project must be testable through the command line.

Use the Gradle wrapper included in the repository:

Linux/macOS:

```bash
./gradlew
```

Windows:

```powershell
.\gradlew
```

Do not require contributors to open Android Studio simply to build or test the project.

Android Studio may be used for development and debugging, but it is not part of the required automated verification process.

---

# 3. INITIAL PROJECT VERIFICATION

Before modifying code, verify that the repository is currently healthy.

Run:

```bash
./gradlew clean
./gradlew test
./gradlew assembleDebug
```

On Windows:

```powershell
.\gradlew clean
.\gradlew test
.\gradlew assembleDebug
```

If these commands fail before your changes, document the existing failure rather than assuming it was caused by your work.

---

# 4. BUILD VERIFICATION

Every change must be compiled.

At minimum:

```bash
./gradlew assembleDebug
```

For a release-oriented verification when appropriate:

```bash
./gradlew assembleRelease
```

The debug build must successfully complete before a feature is considered ready.

A successful build means:

- Compilation succeeds.
- Kotlin/Java compilation succeeds.
- Android resources compile.
- Manifest processing succeeds.
- Generated code succeeds.
- Dependencies resolve.
- APK generation succeeds.

Do not rely only on IDE error highlighting.

The Gradle build is the source of truth.

---

# 5. UNIT TESTING

Run all unit tests:

```bash
./gradlew test
```

If the project contains multiple modules, ensure tests across all relevant modules are executed.

For a specific module:

```bash
./gradlew :module-name:test
```

When adding new logic, add tests for:

- Normal behavior
- Edge cases
- Invalid input
- Empty input
- Boundary values
- Failure conditions
- Expected exceptions/errors
- Regression cases for previously discovered bugs

Do not only test the happy path.

---

# 6. AGENT TESTING

All AI agents must be testable independently from the Android UI.

Each agent should have deterministic test cases around its input/output contract.

For example:

```
Input
    ↓
Agent
    ↓
Expected structured output
```

Tests should verify:

- Correct classification/decision.
- Correct output schema.
- Missing input handling.
- Low-confidence behavior.
- Edge cases.
- No fabricated values.
- No invalid output fields.
- Appropriate fallback behavior.

AI outputs should not be tested only by exact wording when the system does not require exact wording.

Prefer validating semantic properties and structured fields.

For example:

```
intervene = false
level = 0
message = null
```

is more important than requiring one exact sentence.

---

# 7. CONTEXT-AWARE AGENT TESTING

The Context-Aware Agent must be tested against representative scenarios.

Include cases such as:

```
Focus session + productive application
→ FOCUSED

Focus session + prolonged unrelated activity
→ DISTRACTED / POTENTIALLY_DISTRACTED

Break + entertainment application
→ BREAK

Unknown context + ambiguous activity
→ UNKNOWN

Browser + declared research task
→ FOCUSED / CONSISTENT

Short application switch
→ should not automatically become DISTRACTION
```

Test both high-confidence and low-confidence situations.

The agent must not automatically classify an application as distracting solely because of its category.

---

# 8. MINIMAL-INTERVENTION AGENT TESTING

Test at minimum:

### Case 1: No evidence

```
Short potentially distracting activity
+ weak context
→ NO_INTERVENTION
```

### Case 2: Strong distraction

```
Active focus session
+ prolonged inconsistent activity
+ high confidence
→ INTERVENTION
```

### Case 3: Recent intervention

```
Strong distraction
+ intervention sent recently
→ generally NO_INTERVENTION / increased cooldown
```

### Case 4: Ignored intervention

```
Previous intervention ignored
+ same distraction continues
→ do not immediately escalate
```

### Case 5: Break

```
User is on legitimate break
→ NO_INTERVENTION
```

### Case 6: Uncertain context

```
Low context confidence
→ conservative behavior
```

The most important test is that the agent does NOT spam interventions.

---

# 9. REWARD LOOP AGENT TESTING

Test:

### Session completion

```
Meaningful completed focus session
→ SESSION_COMPLETION reward
```

### Recovery

```
Distraction
→ user returns to focus
→ RECOVERY reward
```

### Milestone

```
Meaningful progress threshold reached
→ MILESTONE reward
```

### Trivial interaction

```
Open Undrift
→ NO reward
```

### Duplicate event

```
Same event processed twice
→ should not produce duplicate reward
```

### Break

```
Legitimate break
→ no punishment
```

### Failed/abandoned session

Do not automatically treat every incomplete session as failure.

The system should distinguish between:

```
no meaningful progress
```

and:

```
partial progress
```

where appropriate.

---

# 10. STATIC ANALYSIS

Run Android/Kotlin static checks where configured.

At minimum, attempt:

```bash
./gradlew lint
```

Fix newly introduced:

- Errors
- Resource problems
- Manifest issues
- API compatibility issues
- Unused resources where relevant
- Incorrect permissions
- Security warnings
- Android lifecycle issues

Warnings should be reviewed rather than blindly ignored.

If a warning is intentionally accepted, document why.

---

# 11. CHECK DEPENDENCY AND BUILD HEALTH

Run:

```bash
./gradlew dependencies
```

when dependency-related changes are made.

Verify:

- No accidental dependency additions.
- No unnecessary libraries.
- No conflicting versions.
- No broken repositories.
- No unexpected transitive dependencies.

Do not add a large library for functionality that can reasonably be implemented with existing project dependencies.

---

# 12. ANDROID INSTRUMENTATION TESTS

When Android-specific behavior needs to be tested, use instrumentation tests.

Run:

```bash
./gradlew connectedAndroidTest
```

This requires an Android device or emulator.

The important point is that Android Studio is not required.

The test environment can be started and controlled separately.

Instrumentation tests should cover functionality that cannot reliably be tested as plain JVM unit tests, including:

- Android lifecycle behavior
- Activity/Fragment behavior where applicable
- Permissions
- Notifications
- Local storage
- Android services
- App navigation
- Device-specific APIs
- Usage statistics integration
- Background behavior where applicable

---

# 13. AUTOMATED DEVICE TESTING

Where possible, maintain a reproducible Android test environment.

Preferred options include:

- Android Emulator
- ADB-connected physical Android device
- CI-provided Android emulator

Basic ADB verification:

```bash
adb devices
```

Install the generated APK:

```bash
adb install -r path/to/app-debug.apk
```

Launch the application using the appropriate package/activity configuration.

Then verify:

- Application installs.
- Application launches.
- Application does not immediately crash.
- Main screen loads.
- Required permissions behave correctly.
- Core functionality can be exercised.

---

# 14. LOGCAT VERIFICATION

When testing on a device/emulator, inspect logs:

```bash
adb logcat
```

For a cleaner investigation, filter logs for the application process/package.

Look for:

- FATAL EXCEPTION
- RuntimeException
- SecurityException
- NullPointerException
- ANR
- Permission failures
- Database errors
- Network errors
- Agent/API failures
- Background service failures

Do not assume that "the app opened" means it is working correctly.

---

# 15. CRASH VERIFICATION

A feature must not introduce obvious crashes.

Test:

- Cold start
- Warm start
- Background → foreground
- Rotation/configuration changes where relevant
- Permission denied
- Permission granted
- Empty state
- No network where applicable
- App restart
- Data unavailable
- Unexpected input
- Interrupted session
- Process recreation where relevant

If a crash is discovered, create a regression test whenever practical.

---

# 16. DATABASE / LOCAL STORAGE TESTING

If the application uses local storage/database functionality, verify:

- Fresh installation.
- Existing installation.
- Empty database.
- Existing data.
- Insert.
- Update.
- Delete.
- Migration.
- Invalid/corrupt input where applicable.
- App restart persistence.

Database migrations must be tested explicitly.

A feature is not complete if it works only with a fresh installation.

---

# 17. PERMISSION TESTING

For Android permissions, test both states:

```
Permission granted
Permission denied
```

Also test:

```
Permission revoked after being granted
```

The application should fail gracefully when permissions are unavailable.

Never assume permissions are always granted.

For Undrift, this is particularly important for any functionality involving Android usage data, notifications, or background behavior.

---

# 18. NETWORK/API TESTING

If any component communicates with a backend or AI service, test:

- Successful request.
- Timeout.
- No network.
- Server error.
- Invalid response.
- Malformed response.
- Empty response.
- Rate limiting where relevant.
- Authentication failure.

The application must not crash because an external service fails.

Provide appropriate fallback behavior.

---

# 19. AI FAILURE TESTING

AI components must be treated as unreliable external components.

Test:

```
Valid response
Invalid response
Empty response
Malformed JSON
Missing field
Unexpected field
Timeout
Error
Low confidence
No response
```

The application must have deterministic fallback behavior.

Never allow an AI response to directly crash the application.

---

# 20. UI / END-TO-END TESTING

For critical user flows, test the complete path:

```
Application launch
    ↓
Permission/setup
    ↓
Main screen
    ↓
Focus session
    ↓
Context detection
    ↓
Potential distraction
    ↓
Intervention
    ↓
User recovery
    ↓
Reward
    ↓
Progress update
```

The individual agents passing their tests is not sufficient.

The complete system must also be tested together.

---

# 21. REGRESSION TESTING

Before merging a feature, run the complete relevant test suite.

At minimum:

```bash
./gradlew clean
./gradlew test
./gradlew lint
./gradlew assembleDebug
```

If Android instrumentation tests are configured:

```bash
./gradlew connectedAndroidTest
```

Do not only run the test associated with the file you changed.

Changes in shared code can break unrelated functionality.

---

# 22. CLEAN BUILD VERIFICATION

Periodically verify the project from a clean state.

Run:

```bash
./gradlew clean
./gradlew assembleDebug
```

This catches problems that may be hidden by incremental build artifacts.

A feature that works only because stale generated/build files exist is not considered verified.

---

# 23. AUTOMATION / CI

The project should eventually have a CI pipeline that automatically runs:

```
Checkout repository
        ↓
Set up JDK
        ↓
Gradle dependency resolution
        ↓
Unit tests
        ↓
Lint/static analysis
        ↓
Build debug APK
        ↓
Instrumentation tests
        ↓
Publish test/build artifacts
```

A pull request should not be considered ready if the automated pipeline fails.

The CI environment should be as close as practical to the local command-line workflow.

---

# 24. TESTING AFTER EVERY SIGNIFICANT CHANGE

The following changes require verification:

- Agent prompt changes
- Agent logic changes
- Android service changes
- Database changes
- UI changes
- Permission changes
- Dependency changes
- Build configuration changes
- API/backend changes
- Navigation changes
- Notification changes
- Background processing changes

AI prompt changes count as functional changes.

A prompt can change application behavior even when no Kotlin/Java code changes.

Therefore prompt changes must also be tested.

---

# 25. BUG REPORTING

When a test fails, record:

```
Bug:
Expected:
Actual:
Steps to reproduce:
Environment:
Relevant logs:
Likely cause:
Fix:
Regression test:
```

Do not simply report:

"Doesn't work."

The goal is to make every bug reproducible.

---

# 26. DEFINITION OF DONE

A feature is considered DONE only when:

- Code compiles.
- Relevant unit tests pass.
- Relevant integration tests pass.
- Static analysis passes.
- Debug APK builds successfully.
- New edge cases are covered.
- Existing functionality still works.
- Android-specific behavior has been tested where applicable.
- No known critical crash exists.
- AI failure cases have been considered where applicable.
- The change has been manually sanity-checked when automated testing cannot cover it.
- Any known limitations are documented.

"Works on my machine" is not sufficient.

---

# 27. FINAL VERIFICATION COMMANDS

Before submitting a significant feature, run the appropriate commands:

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

On Windows:

```powershell
.\gradlew clean
.\gradlew test
.\gradlew lint
.\gradlew assembleDebug
.\gradlew connectedAndroidTest
```

If any command fails:

1. Determine whether the failure is pre-existing.
2. If caused by the current change, fix it.
3. Re-run the failing test.
4. Re-run the broader verification suite.
5. Document unavoidable limitations.

---

# FINAL RULE

Never declare a feature complete solely because the feature appears to work manually.

Undrift should be treated as a complete software system.

Verify the:

- Code
- Build
- Agents
- Android integration
- Data layer
- UI
- External services
- Failure paths
- Regression behavior

The goal is not merely:

"Does it work?"

The goal is:

"Can we repeatedly prove that it works, detect when it stops working, and identify exactly what broke?"