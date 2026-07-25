<!--
Thanks for contributing to MediTrack!
Please read CONTRIBUTING.md if you have not already.
Delete sections that genuinely do not apply — but prefer filling them in.
-->

## What does this change?

<!-- One or two sentences. What is different after this PR? -->

## Why?

<!--
The most important section. The diff shows what changed; explain why this is
the right change. If it fixes an issue, link it: "Closes #12".
-->

Closes #

## Type of change

- [ ] Bug fix — non-breaking change that fixes an issue
- [ ] New feature — non-breaking change that adds functionality
- [ ] Breaking change — existing behaviour changes
- [ ] Documentation only
- [ ] Refactor / internal cleanup, no behaviour change
- [ ] Tests only

## How was this tested?

<!--
Be specific. "Ran the suite" is less useful than naming the assertions you
added and the case they would catch.
-->

```
# paste the tail of: java -cp out com.airtribe.meditrack.Main --runTests
```

- [ ] Full suite passes locally
- [ ] I added assertions covering this change
- [ ] I manually walked the affected menu path (state which: ______)

## Checklist

- [ ] **No new third-party runtime dependencies** — JDK standard library only
- [ ] Code follows the conventions in [CONTRIBUTING.md](../CONTRIBUTING.md)
- [ ] Public types and methods have JavaDoc explaining *why*
- [ ] Any closeable resource uses `try-with-resources`
- [ ] No `System.out` outside `Main`, `TestRunner` and the observers
- [ ] Docs updated if behaviour changed
- [ ] **No real patient data** anywhere in this PR — code, fixtures, or screenshots
- [ ] One logical change; no unrelated reformatting

## Screenshots / console output

<!-- For UI or menu changes, paste the before and after console output. -->

## Anything reviewers should look at closely?

<!--
Call out the risky part. If you were unsure about a decision, say so here —
that is the fastest way to get a useful review.
-->
