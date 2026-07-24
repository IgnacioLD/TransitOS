# Contributing to TransitOS

Thanks for your interest in making TransitOS better. This guide covers
everything you need to start contributing, from your first build to shipping a
pull request.

## Code of conduct

Be kind and constructive. Assume good intent. We are building a calm, private
transit app, and the community around it should feel the same way.

## Getting set up

1. **Fork and clone** the repository.
2. Make sure you have **JDK 17** and **Android SDK 35** (Platform 35 + Build
   Tools) installed.
3. Open the project in Android Studio (Ladybug or newer recommended) or work
   from the CLI.

   ```bash
   ./gradlew :app:assembleDebug
   ```

4. Pick an issue tagged [`good first issue`](https://github.com/IgnacioLD/TransitOS/labels/good%20first%20issue)
   for a gentle start.

Read [`ARCHITECTURE.md`](./ARCHITECTURE.md) before writing code. It explains the
module graph and the rules that keep the layers clean.

## How to make changes

1. Create a branch from `master`:
   `git checkout -b feat/short-description` (or `fix/`, `docs/`, `i18n/`).
2. Make your change. Keep commits focused and write clear messages in the
   imperative mood ("Add transfer buffer setting").
3. Make sure the project builds and existing tests pass:

   ```bash
   ./gradlew assembleDebug test
   ```

4. Open a pull request against `master` and fill in the template.

## Where things live

| You want to...                     | Then look at                                              |
| ---------------------------------- | --------------------------------------------------------- |
| Change a screen                    | `feature-<name>/`                                         |
| Add a shared composable            | `core-ui/`                                                |
| Change the theme                   | `core-design/`                                            |
| Change a domain model or contract  | `core/`                                                   |
| Add or change a preference         | `core-data/` + the relevant `core/` repository contract   |
| Change networking                  | `core-network/` or the relevant `provider-*/`             |
| Add a new transit operator         | a new `provider-<operator>/` module                       |
| Change navigation or DI wiring     | `app/`                                                    |

### Adding a feature module

1. Create `feature-<name>/` with `build.gradle.kts` mirroring an existing
   feature module.
2. Register it in `settings.gradle.kts` and add it as a dependency of `:app`.
3. Put your Koin module in `di/` and load it from `TransitOSApplication`.
4. Add a route in `app/.../TransitOSApp.kt`.

### Adding a transit operator

1. Create `provider-<operator>/` depending on `:core` and `:core-network`.
2. Implement `TransitRepository` against the operator's data source.
3. Map all network DTOs to domain models. DTOs never leave the module.
4. Expose a Koin module and register it in `:app`.

## Internationalisation (i18n)

TransitOS ships in English, Spanish, and Valencian. **Never hardcode a
user-facing string.** Always go through `stringResource()`:

```kotlin
// Bad
Text("Buscar")

// Good
Text(stringResource(R.string.search))
```

Strings live in each module's `res/values/strings.xml` (default: English), with
translations in `values-es/` (Spanish) and `values-ca/` (Valencian). When you
add a string, add it to all three locales, or leave a clear note for translators
in your PR.

## Code style

The repo includes an [`.editorconfig`](./.editorconfig). Most modern editors and
`ktlint` follow it automatically. In short:

- Kotlin, 4-space indent, UTF-8, trailing newline.
- `kotlin.code.style=official`.
- Name packages `app.transitos.<module-suffix>`.
- Keep composables small, stateless, and hoisted.

## Testing

- Pure logic (mappers, parsers) gets unit tests in the module's `src/test/`.
- Tests use JUnit, Truth, and Turbine. Mirror the source package structure.

If your change is non-trivial, add a test. Bug fixes should include a regression
test where practical.

## Commit and PR conventions

- Branch prefix: `feat/`, `fix/`, `docs/`, `i18n/`, `chore/`, `refactor/`.
- Commit message: imperative, concise ("Fix planner crash on empty results").
- Sign off every commit with `git commit -s` (see the DCO section above).
- One logical change per PR. Split unrelated changes into separate PRs.
- Reference the issue number in the PR description ("Closes #123").

## Reporting bugs

Use the GitHub issue tracker. The bug report template asks for your device,
Android version, the operator you were using, and steps to reproduce. The more
detail you give, the faster we can help.

## Contributor agreement (DCO + CLA)

TransitOS is dual-licensed: a free AGPL version for the community, plus an
optional commercial license for organizations that cannot comply with AGPL. To
keep this model clean, every contribution is accepted under two short agreements
that protect everyone:

1. **The DCO (Developer Certificate of Origin)**: every commit is signed off
   with `git commit -s`, confirming you wrote it and may contribute it. See
   [`.github/DCO.md`](./.github/DCO.md).
2. **The CLA (Contributor License Agreement)**: by opening a pull request you
   agree to the [CLA](./.github/CLA.md). **You keep your copyright.** It is a
   license, not an assignment. It only grants the project the right to offer your
   contribution under both the AGPL and a commercial license, and it promises
   that a free, open-source version of TransitOS will always exist.

Neither agreement takes your copyright away or hides your work. They exist so
the project can remain free forever and still have a sustainable path.

### Signing your commits

```bash
git commit -s -m "Add transfer buffer setting"
```

This adds a `Signed-off-by: Your Name <you@example.com>` trailer. Our CI checks
that every commit in a pull request has one. To fix existing commits:

```bash
git rebase --signoff master
git push --force-with-lease
```

Questions? Open a discussion or an issue. Happy hacking.
