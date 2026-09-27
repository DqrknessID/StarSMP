# Tutorial Resource Pack StarSMP (1.21.5+)

Tutorial ini menjelaskan cara mengonfigurasi Resource Pack Anda agar sesuai dengan sistem **Data Component** (Custom Model Data berbasis `floats`) yang digunakan pada plugin StarSMP untuk versi Minecraft 1.21.5 ke atas.

## 1. Struktur Folder
Pastikan file item dasar Minecraft berada di folder `minecraft/items/` (bukan `minecraft/models/item/` lagi untuk file konfigurasi utama).

```text
ResourcePack/
├── pack.mcmeta
└── assets/
    ├── minecraft/
    │   ├── items/
    │   │   └── nether_star.json  <-- Konfigurasi utama CustomModelData
    └── starsmp/
        ├── models/
        │   └── item/
        │       ├── fire_star.json
        │       └── essence.json
        └── textures/
            └── item/
                ├── fire_star.png
                └── essence.png
```

## 2. Konfigurasi `nether_star.json` (1.21.5+)
Di versi 1.21.5+, kita tidak lagi menggunakan `overrides`. Sebagai gantinya, kita menggunakan model `minecraft:select` yang merujuk pada `minecraft:custom_model_data`.

Gunakan file berikut sebagai dasar:

```json
{
  "model": {
    "type": "minecraft:select",
    "property": "minecraft:custom_model_data",
    "index": 0,
    "cases": [
      {
        "when": "101.0",
        "model": "starsmp:item/fire_star"
      },
      {
        "when": "200.0",
        "model": "starsmp:item/essence"
      },
      {
        "when": "201.0",
        "model": "starsmp:item/reparation_tool"
      },
      {
        "when": "202.0",
        "model": "starsmp:item/reroll_token"
      },
      {
        "when": "203.0",
        "model": "starsmp:item/star_fragment"
      }
    ],
    "fallback": {
      "type": "minecraft:model",
      "model": "minecraft:item/nether_star"
    }
  }
}
```

### Penjelasan Parameter:
*   **`property": "minecraft:custom_model_data"`**: Mengambil data dari komponen CustomModelData.
*   **`index": 0`**: Karena plugin StarSMP menggunakan `setFloats(List.of(...))`, angka model data kita tersimpan di **index ke-0** dari list tersebut.
*   **`when": "101.0"`**: Nilai yang kita cari. **Harus menyertakan `.0`** karena tipenya adalah Float (desimal).
*   **`model": "starsmp:item/fire_star"`**: Lokasi file model kustom Anda di `assets/starsmp/models/item/fire_star.json`.
*   **`fallback`**: Model yang digunakan jika angka data tidak cocok (Default Nether Star).

## 3. Map Nilai Model Data Default
Berikut adalah nilai model data default yang ada di plugin:

| Item | Model Data Value (`when`) |
| :--- | :--- |
| **Essence** | `200.0` |
| **Reparation Tool** | `201.0` |
| **Reroll Token** | `202.0` |
| **Star Fragment** | `203.0` |
| **Star Items** | Sesuai dengan Enum `StarType` (contoh: Fire = `101.0`) |

## 4. `pack.mcmeta`
Pastikan `pack_format` Anda sudah benar (minimal **46** untuk versi 1.21.4-1.21.5).

```json
{
  "pack": {
    "description": "StarSMP Official Resource Pack",
    "pack_format": 46
  }
}
```

---
**Catatan:** Jika Anda mengubah nilai di `config.yml` plugin StarSMP, Anda harus menyesuaikan nilai `when` di file `.json` ini agar texture muncul dengan benar.
