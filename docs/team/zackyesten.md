# Zheng Yiting - Project Portfolio Page

## Overview

Jaweeper is a Java command-line Minesweeper game for players who prefer keyboard commands. It supports local player profiles, different board sizes and saved games. My main responsibilities were command processing, profile management and the shared runtime/storage foundation.

## Summary of contributions

- **Code contributed:** [RepoSense contribution view](https://nus-cs2113-ay2627-s1.github.io/tp-dashboard/?search=zackyesten) | [GitHub: zackyesten](https://github.com/zackyesten).

- **Command processing and integration** ([#6](https://github.com/AY2627S1-CS2113-W10-4/tp/pull/6), [#21](https://github.com/AY2627S1-CS2113-W10-4/tp/pull/21)): introduced the application, command and parser skeleton; implemented the input loop and initial help, exit, new-game and board-display commands. Added coordinate validation and conversion to zero-based indices, and connected teammates' reveal/flag/unflag commands. Invalid input reports an error without terminating the command loop.

- **Profile and storage foundation** ([#34](https://github.com/AY2627S1-CS2113-W10-4/tp/pull/34)): implemented `Profile`, `ProfileManager`, startup creation/selection and shared UTF-8 `Storage`. Stable UUIDs keep player data independent of display names. Registry updates are persisted before memory changes; malformed registries are reported without overwriting them. Added `GameSession` and central command registration so features share the current board and player, with integration contracts in [ProfileIntegration.md](https://github.com/AY2627S1-CS2113-W10-4/tp/blob/master/docs/ProfileIntegration.md).

- **In-game profile management** ([#55](https://github.com/AY2627S1-CS2113-W10-4/tp/pull/55)): completed listing, creation, selection, renaming and deletion. Integrated switching with the shared save tracker to block loss of unfinished progress and reset player-specific game state. Renaming preserves identity and existing statistics; deletion retains associated files. Added 12 tests covering persistence, invalid input, failed writes and cross-profile state isolation.

- **Profile reliability fixes** ([#68](https://github.com/AY2627S1-CS2113-W10-4/tp/pull/68), [issue #67](https://github.com/AY2627S1-CS2113-W10-4/tp/issues/67)): fixed runtime name parsing so whitespace, duplicate checks and length limits agree with startup. Bound deletion confirmation to the warned UUID, name and list number, preventing a repeated confirmation from deleting another player after renumbering. Added 12 regression tests, verified they failed before the fixes and passed afterwards, and checked the packaged JAR across a restart.

- **Testing and diagnostics:** wrote parser/core-command tests, gameplay-routing integration tests, and profile/startup/storage tests using temporary directories. Added parser invariants and session/command logging in [#27](https://github.com/AY2627S1-CS2113-W10-4/tp/pull/27), while retaining exception-based validation for invalid user input.

- **Documentation:** wrote the UG's [startup profile instructions](https://ay2627s1-cs2113-w10-4.github.io/tp/UserGuide.html#creating-or-selecting-a-player) and [runtime profile commands](https://ay2627s1-cs2113-w10-4.github.io/tp/UserGuide.html#managing-profiles-during-a-game), including persistence and deletion rules. Documented command processing and profile design in [#35](https://github.com/AY2627S1-CS2113-W10-4/tp/pull/35) and expanded the DG in [#48](https://github.com/AY2627S1-CS2113-W10-4/tp/pull/48). Contributed PlantUML class, object and sequence diagrams with exported PNGs in [#54](https://github.com/AY2627S1-CS2113-W10-4/tp/pull/54), addressing my portion of the tutor's [#52 feedback](https://github.com/AY2627S1-CS2113-W10-4/tp/issues/52). The current DG describes [profile switching](https://ay2627s1-cs2113-w10-4.github.io/tp/DeveloperGuide.html#switching-profiles).

- **Team contributions:** coordinated command registration and the shared session/storage interfaces to support parallel development, integrated teammates' commands, and published the [v1.0 release](https://github.com/AY2627S1-CS2113-W10-4/tp/releases/tag/v1.0).

- **Review contributions:** gave [code-specific feedback on #25](https://github.com/AY2627S1-CS2113-W10-4/tp/pull/25#issuecomment-6166280055) about board invariants and logging. Submitted a [DG review for team T11-3](https://github.com/NUS-CS2113-AY2627-S1/tp/pull/18#pullrequestreview-5678269312), including UML notation and a replenishment example inconsistent with duplicate-item validation.
