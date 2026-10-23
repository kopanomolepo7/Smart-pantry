# Smart Pantry Manager

A Java Android application that helps reduce food waste by tracking the
ingredients a user has at home and suggesting recipes they can cook using
**strictly** those ingredients — a recipe is only suggested if every
required ingredient is present in the pantry at or above the required
quantity.

> Built for Mobile App Development 700, Richfield Graduate Institute of
> Technology.

## Design

The interface uses a warm "kitchen ledger" identity rather than a
generic green grocery-app look: ink/parchment tones, one ochre accent
color, hairline-stroked cards instead of drop shadows, serif titles
paired with sans-serif body text, and a small set of hand-drawn vector
icons (no stock Android glyphs). A shared type scale (`styles.xml`) and
color-role system (`colors.xml` / `values-night/colors.xml`) keep every
screen consistent and support a real dark mode via Settings →
Appearance.

## Main features

- **Pantry management** — add, edit and delete ingredients (name,
  quantity, unit, optional expiry date), backed by SQLite so data
  survives closing and reopening the app.
- **Suggested Recipes**, split into two sections:
  - **Ready to Cook** — the strict-matching list; only recipes where
    every ingredient is present in sufficient quantity are shown.
  - **Almost There** (optional stretch feature) — recipes missing
    exactly one ingredient, clearly separated from the strict list,
    each showing which ingredient is missing.
- **18 seeded recipes** with realistic, overlapping ingredients so the
  matching logic has real variety to work with (Scrambled Eggs, Tomato
  Pasta, Chicken Stir Fry, Cheesy Garlic Bread, and more).
- **"Cook this recipe"** on the Recipe Detail screen — deducts the used
  quantities from the pantry (converting units where needed), removing
  an ingredient entirely once it hits zero.
- **Favorites** — star a recipe on its detail screen; filter Suggested
  Recipes down to favorites only.
- **Pantry stats banner** — a running count of ingredients, recipes
  ready to cook, and items expiring soon.
- **Settings** — expiring-soon alert toggle, a default unit for new
  ingredients, and an appearance preference (Follow system / Light /
  Dark).
- Input validation on the Add/Edit Ingredient form (empty name,
  non-numeric or non-positive quantity, invalid date format).
- Empty-state messaging (empty pantry, no recipe matches) instead of a
  blank screen.

## Database choice: SQLite

SQLite (via `SQLiteOpenHelper`) was chosen because this is a fully local,
single-user application with no need for network sync or multi-device
access. It requires no external services or accounts, is simple to
demonstrate and explain end-to-end, and is directly supported by the
Android SDK. Three tables are used:

- `pantry_items` — the user's own ingredients (full CRUD)
- `recipes` — static recipe catalogue, seeded on first run
- `recipe_ingredients` — one row per (recipe, required ingredient) pair,
  so the strict-matching algorithm can check each required ingredient
  individually rather than parsing a free-text list

## Requirements

- Android Studio (Hedgehog or later recommended)
- Android SDK with API level 34 (compile/target), minimum supported
  API level 24 (Android 7.0)
- A device or emulator running Android 7.0+

## Setup instructions

1. Clone or download this repository.
2. Open the `SmartPantryManager` folder in Android Studio (**File → Open**
   and select the folder — do not open individual files).
3. Let Gradle sync finish (Android Studio will download the required
   dependencies automatically).
4. Run the app on an emulator or a connected physical device using the
   green Run button.

## How to run

On first launch the app creates its local database and seeds it with 18
recipes. Add a few pantry ingredients from the **Pantry** tab, then check
the **Recipes** tab — recipes appear only once you have every ingredient
they need at a sufficient quantity.

## Project structure

```
app/src/main/java/com/richfield/smartpantrymanager/
├── MainActivity.java              # hosts toolbar + bottom nav + fragment container
├── database/DatabaseHelper.java   # schema, CRUD, recipe seeding
├── model/                         # PantryItem, Recipe, RecipeIngredient
├── util/IngredientMatcher.java    # strict-matching algorithm
└── ui/
    ├── pantry/                    # Pantry List + Add/Edit Ingredient
    ├── recipes/                   # Suggested Recipes + Recipe Detail
    └── settings/                  # Settings fragment
```
