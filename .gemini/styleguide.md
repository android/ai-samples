# Android samples Style Guide

## Jetpack Compose Style Guide

### Compose and Activity 
* Unless completely necessary, apps using Jetpack Compose should have a single `Activity`
* Choose `ComponentActivity` over `AppCompatActivity` when possible:

    `ComponentActivity` has all you need for a Compose-only app.

    Use `AppCompatActivity` only if you need: `AppCompat APIs`, `AndroidView` or `Fragments`
* Application must support **edge-to-edge display**.

### Composable functions
* A `@Composable` function returning `Unit` should be named using PascalCase and a noun
* Composables that return values should use the standard Kotlin Conventions for function naming, starting with lowercase.
* `@Composable` functions should either emit content into the composition or return a value, but not both.
* Any `@Composable` function that internally uses `remember{}` and returns a mutable object, should be prefixed with “remember”
* If a Composable has a content parameter, its value should be set by moving the lambda out of the Composable’s parentheses
* Composables should have names that describe what they do
ex: naming a Composable `DeleteButton` vs naming it `Button`

### Composable previews
* Try to use `@Preview` Composables when possible
* Separate `@Previews` from Composable implementation


### Misc. Jetpack compose good practices:
* Use `items` keyword rather than a `for loop` to iterate through a list items
* Break up large Composables into smaller sub Composables for **performance**, **reusability** and **readability**
* **Separate code appropriately** - Compose code should deal with UI, state-holder classes should deal with logic. If a file is large, try to split unrelated functions/classes/variables into a separate file.
* Only use `when` statements when there are multiple cases


