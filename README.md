# Food Calc - backend

Calories of what you eat, from the barcode on the package. This is the small REST API (Spring Boot) behind the
app: given a barcode it asks [Open Food Facts](https://world.openfoodfacts.org) and answers with name, brand and
nutrients per 100 g. The app (scanner, grams, daily total) is in
[food-calc-app](https://github.com/RR2000/food-calc-app).

Demo: https://kcal.raoulrondinella.dev

## API

`GET /api/products/{barcode}` (6 to 14 digits: EAN-13, EAN-8, UPC-A...)

```json
{
  "barcode": "3017620422003",
  "name": "Nutella",
  "brand": "Ferrero",
  "quantity": "400 g",
  "imageUrl": "https://images.openfoodfacts.org/...",
  "per100g": { "kcal": 539.0, "protein": 6.3, "carbs": 57.5, "sugars": 56.3, "fat": 30.9, "salt": 0.1 }
}
```

`404` when Open Food Facts does not know the barcode, `400` when it is not a barcode, `502` when Open Food
Facts does not answer. A missing nutrient is `null`; kcal are computed from kJ when only those are given.
Answers (found and not found) are kept in memory for 12 hours, so the same barcode is asked only once.

Health: `/actuator/health`.

## Run

```bash
mvn verify            # tests, against a fake Open Food Facts on a local port
mvn spring-boot:run   # API on :8080
```

| variable | default | |
|---|---|---|
| `FOODCALC_OFF_URL` | `https://world.openfoodfacts.org` | Open Food Facts API |
| `FOODCALC_CORS_ORIGINS` | `http://localhost:8081` | only for a web app on another origin (the Expo dev server) |
| `PORT` | `8080` | |

No database: the food diary stays on the user's device.

## Image

The CI publishes `ghcr.io/rr2000/food-calc-backend:main` (and `:sha-<commit>`) at every push on `main`.

Data from Open Food Facts, available under the
[Open Database License](https://opendatacommons.org/licenses/odbl/1-0/).
