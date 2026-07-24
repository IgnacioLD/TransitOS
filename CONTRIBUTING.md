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
- One logical change per PR. Split unrelated changes into separate PRs.
- Reference the issue number in the PR description ("Closes #123").

## Reporting bugs

Use the GitHub issue tracker. The bug report template asks for your device,
Android version, the operator you were using, and steps to reproduce. The more
detail you give, the faster we can help.

## Licensing

By contributing, you agree that your changes will be released under the
[GNU AGPL-3.0](./LICENSE). Contributions under any other terms cannot be
accepted.

Questions? Open a discussion or an issue. Happy hacking.
