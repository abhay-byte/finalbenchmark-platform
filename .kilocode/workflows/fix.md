# fix.md Workflow

## Purpose
Quick workflow for identifying and fixing bugs or issues in the codebase.

## Usage
Type `/fix` in the chat to invoke this workflow.

## Workflow Steps

### Step 1: Identify the Issue
- Ask the user to describe the bug or error they're experiencing
- Request relevant error messages, stack traces, or logs
- Determine which files or components are affected

### Step 2: Analyze the Code
- Examine the relevant files and code sections
- Identify the root cause of the issue
- Check for common patterns: null pointer exceptions, type mismatches, logic errors, missing dependencies

### Step 3: Propose Solution
- Explain what's causing the issue in clear terms
- Propose one or more solutions
- Discuss trade-offs if multiple approaches are available

### Step 4: Implement Fix
- Apply the fix to the affected files
- Ensure code follows existing patterns and conventions
- Add comments explaining the fix if necessary

### Step 5: Verify & Test
- How to build :- gradlew.bat assembleDebug
- Suggest test cases to verify the fix works
- Check for potential side effects or edge cases
- Recommend additional tests if needed BUILD AND RUN APK
- Verify:- dir app\build\outputs\apk\debug\
- Install APK:- adb install app\build\outputs\apk\debug\app-debug.apk

### Step 6: Commit all changes and push to github

```bash
git add .
git commit -m "Fix: [brief description of the fix]"
git push origin main
```

