# SRC-012 — Extraction notes: Google Play User Data policy (privacy policy)

Source: https://support.google.com/googleplay/android-developer/answer/10144311 (read 2026-09-28).

| Obligation taken | Wording on the page | Used by |
|---|---|---|
| Every app needs a privacy policy, even one that collects nothing | "Apps that do not access any personal and sensitive user data must still submit a privacy policy." | the release checklist (REQ-048) |
| The policy must be reachable in two places | "All apps must post a privacy policy link in the designated field within Play Console, and a privacy policy link or text within the app itself." | the in-app privacy text (REQ-049); the hosted copy for the Play Console field (REQ-048) |
| The Data safety form must be accurate | the Data safety section is filled in from the same facts | REQ-048 ("no data collected") |

**Ambiguity:** the policy changes over time; re-check before the first
release. A text inside the app satisfies the in-app half without any
network use.
