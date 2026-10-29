# Zheng Yiting — Project Portfolio Page

## Overview

Jaweeper is a Java command-line Minesweeper game with local player profiles, configurable
boards and named saves. I worked on command processing and the shared profile/session
foundation used by the team's features.

## Summary of contributions

- **Code:** [RepoSense contribution view](https://nus-cs2113-ay2627-s1.github.io/tp-dashboard/?search=zackyesten).
- **Command execution:** introduced the application/command/parser skeleton in
  [#6](https://github.com/AY2627S1-CS2113-W10-4/tp/pull/6), then implemented core commands,
  input validation and routing in [#21](https://github.com/AY2627S1-CS2113-W10-4/tp/pull/21).
  The parser converts user coordinates to zero-based command arguments; invalid input
  produces a message while leaving the command loop usable.
- **Profiles and integration:** implemented `Profile`, `ProfileManager`, shared UTF-8
  `Storage`, startup creation/selection and `GameSession` integration in
  [#34](https://github.com/AY2627S1-CS2113-W10-4/tp/pull/34). Stable UUIDs keep data paths
  independent of display names. Registry changes are written before replacing memory,
  and malformed existing registries are reported rather than overwritten.
  Extended this foundation with runtime profile listing/creation/selection/renaming/deletion
  for [#33](https://github.com/AY2627S1-CS2113-W10-4/tp/issues/33). Switching requires saved
  progress and resets board history, timing and save confirmations; renaming preserves
  existing statistics, while confirmed deletion retains associated files.
- **Testing and diagnostics:** added parser, command, profile storage and startup tests,
  including invalid input, temporary-directory I/O cases and cross-profile state isolation.
  Added assertions for internal
  assumptions and logging for command processing in
  [#27](https://github.com/AY2627S1-CS2113-W10-4/tp/pull/27).
- **User Guide:** documented startup profile selection, local profile persistence,
  help/board usage, the current exit behaviour and runtime profile-management examples.
- **Developer Guide:** wrote the command/profile implementation section in
  [#35](https://github.com/AY2627S1-CS2113-W10-4/tp/pull/35), expanded documentation in
  [#48](https://github.com/AY2627S1-CS2113-W10-4/tp/pull/48), and revised my diagrams and
  explanations in response to [#52](https://github.com/AY2627S1-CS2113-W10-4/tp/issues/52).
  My PlantUML diagrams cover command coordination, the profile class structure, a
  selected-player object snapshot and flag-command execution.
- **Team work:** coordinated command registrations and the shared `GameSession`/storage
  interfaces, and published the [v1.0 release](https://github.com/AY2627S1-CS2113-W10-4/tp/releases/tag/v1.0).
- **Reviews:** provided feedback on logging/assertions in
  [#25](https://github.com/AY2627S1-CS2113-W10-4/tp/pull/25) and reviewed the T11-3 team's
  DG in [course PR #18](https://github.com/NUS-CS2113-AY2627-S1/tp/pull/18), including
  UML notation and a stock-replenishment example that conflicted with duplicate-item validation.

## Documentation extracts

My main design explanation is [Command processing and profile sessions](../DeveloperGuide.md#command-processing-and-profile-sessions).
The [profile class diagram](../images/zack-profile-classes.png),
[object snapshot](../images/zack-profile-objects.png) and
[command sequence](../images/zack-command-sequence.png) explain different views of this implementation.
My startup instructions are in [Creating or selecting a player](../UserGuide.md#creating-or-selecting-a-player).
