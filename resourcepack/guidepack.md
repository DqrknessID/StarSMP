# 📦 StarSMP Resource Pack — Panduan Pembuatan Custom Model & Texture

> Panduan ini menjelaskan **di mana** menaruh file texture, model, dan folder pendukung untuk semua item custom di StarSMP.

---

## 🗂️ Struktur Folder Resourcepack

```
resourcepack/
├── pack.mcmeta
└── assets/
    ├── minecraft/
    │   └── models/
    │       └── item/
    │           ├── mace.json               ← override CMD 301 → Cosmic Judgement
    │           ├── netherite_helmet.json   ← override CMD 300 → Crown of Astral Supremacy
    │           └── nether_star.json        ← overrides CMD 101–110 → 10 Stars
    └── starsmp/
        ├── items/                          ← Item definition files (1.21.4+)
        │   ├── cosmic_judgement.json
        │   ├── crown_astral_supremacy.json
        │   ├── star_aqualith.json
        │   ├── star_pyrelion.json
        │   ├── star_zephyros.json
        │   ├── star_gravaryn.json
        │   ├── star_voltyra.json
        │   ├── star_verdaryn.json
        │   ├── star_umbryx.json
        │   ├── star_luminary.json
        │   ├── star_aetherion.json
        │   └── star_chronovar.json
        ├── models/
        │   └── item/
        │       ├── cosmic_judgement.json   ← 3D model Cosmic Judgement (MACE)
        │       ├── crown_astral_supremacy.json ← 3D model Crown (HELMET)
        │       ├── star_aqualith.json
        │       ├── star_pyrelion.json
        │       ├── star_zephyros.json
        │       ├── star_gravaryn.json
        │       ├── star_voltyra.json
        │       ├── star_verdaryn.json
        │       ├── star_umbryx.json
        │       ├── star_luminary.json
        │       ├── star_aetherion.json
        │       └── star_chronovar.json
        ├── textures/
        │   └── item/
        │       ├── cosmic_judgement.png    ← Texture 2D atau baked 3D
        │       ├── crown_astral_supremacy.png
        │       ├── star_aqualith.png
        │       ├── star_pyrelion.png
        │       ├── star_zephyros.png
        │       ├── star_gravaryn.png
        │       ├── star_voltyra.png
        │       ├── star_verdaryn.png
        │       ├── star_umbryx.png
        │       ├── star_luminary.png
        │       ├── star_aetherion.png
        │       └── star_chronovar.png
        └── sounds/
            └── (custom sounds jika ada)
```

---

## ⚔️ Cosmic Judgement — Mace (Custom 3D Model)

| Property | Value |
|----------|-------|
| **Base Item** | `MACE` |
| **Custom Model Data** | `301` |
| **Override File** | `assets/minecraft/models/item/mace.json` |
| **Model File** | `assets/starsmp/models/item/cosmic_judgement.json` |
| **Texture File** | `assets/starsmp/textures/item/cosmic_judgement.png` |

### Cara buat 3D Model Mace (Blockbench):
1. Buka **Blockbench** → *New Java/Bedrock Item*
2. Desain model berbentuk mace / gada (heavy weapon)
3. Set **Display → First Person / Third Person / GUI** transforms
4. Export ke JSON → simpan di `assets/starsmp/models/item/cosmic_judgement.json`
5. Ganti `"parent": "minecraft:item/handheld"` dengan geometri custom Blockbench-mu
6. Texture PNG (32x32 atau 64x64) taruh di `assets/starsmp/textures/item/cosmic_judgement.png`

### Format model JSON Cosmic Judgement (stub saat ini):
```json
{
    "parent": "minecraft:item/handheld",
    "textures": { "layer0": "starsmp:item/cosmic_judgement" },
    "display": { ... }
}
```
> ⚠️ Ganti `parent` + `textures` dengan hasil export Blockbench untuk 3D model asli.

---

## 👑 Crown of Astral Supremacy — Helmet (Custom 3D Model)

| Property | Value |
|----------|-------|
| **Base Item** | `NETHERITE_HELMET` |
| **Custom Model Data** | `300` |
| **Override File** | `assets/minecraft/models/item/netherite_helmet.json` |
| **Model File** | `assets/starsmp/models/item/crown_astral_supremacy.json` |
| **Texture File** | `assets/starsmp/textures/item/crown_astral_supremacy.png` |

### Cara buat 3D Model Crown di Blockbench:
1. Buka **Blockbench** → *Java Block/Item*
2. Desain **mahkota** (crown shape, bisa pakai elemen kubus melingkar)
3. Atur display transforms untuk posisi di kepala player
4. Export JSON → simpan di `assets/starsmp/models/item/crown_astral_supremacy.json`
5. Texture di `assets/starsmp/textures/item/crown_astral_supremacy.png`

> 💡 **Tip**: Untuk armor 3D yang muncul di kepala player secara in-game, gunakan **Optifine** atau **CIT Resewn** dengan `type=armor` atau custom entity texture. Tanpa mod, only item model (held/GUI) yang bisa diubah via resourcepack vanilla.

### Armor Texture (Opsional, butuh CIT Resewn / Optifine):
Jika ingin texture crown tampil saat dipakai, buat folder CIT:
```
assets/minecraft/optifine/cit/
└── crown_astral_supremacy/
    ├── crown_astral_supremacy.properties
    ├── crown_astral_supremacy.png      ← layer 1 armor texture (64x32)
    └── crown_astral_supremacy_2.png    ← layer 2 (leggings overlay, bisa kosong)
```
**Isi `.properties`:**
```properties
type=armor
items=netherite_helmet
nbt.PublicBukkitValues.starsmp\:custom_item_type=CROWN_ASTRAL_SUPREMACY
texture.armor_layer_1=crown_astral_supremacy
```

---

## ⭐ Stars — Nether Star Items (Custom Textures)

Semua Star menggunakan `NETHER_STAR` sebagai base item dengan `custom_model_data` 101–110.

| Star | CMD | Model File | Texture File |
|------|-----|-----------|-------------|
| Aqualith 🌊 | 101 | `star_aqualith.json` | `star_aqualith.png` |
| Pyrelion 🔥 | 102 | `star_pyrelion.json` | `star_pyrelion.png` |
| Zephyros 🌪 | 103 | `star_zephyros.json` | `star_zephyros.png` |
| Gravaryn 🪨 | 104 | `star_gravaryn.json` | `star_gravaryn.png` |
| Voltyra ⚡ | 105 | `star_voltyra.json` | `star_voltyra.png` |
| Verdaryn 🌿 | 106 | `star_verdaryn.json` | `star_verdaryn.png` |
| Umbryx 🌑 | 107 | `star_umbryx.json` | `star_umbryx.png` |
| Luminary ✨ | 108 | `star_luminary.json` | `star_luminary.png` |
| Aetherion 🌌 | 109 | `star_aetherion.json` | `star_aetherion.png` |
| Chronovar ⏳ | 110 | `star_chronovar.json` | `star_chronovar.png` |

### Cara buat texture Star:
1. Buat image **32x32 PNG** atau **16x16 PNG**
2. Desain sesuai tema Star (biru air untuk Aqualith, merah api untuk Pyrelion, dst.)
3. Simpan di `resourcepack/assets/starsmp/textures/item/star_<nama>.png`
4. Model JSON sudah ada (stub pakai `"parent": "minecraft:item/generated"`)

### Cara pakai texture di model (sudah otomatis):
```json
{
    "parent": "minecraft:item/generated",
    "textures": {
        "layer0": "starsmp:item/star_aqualith"
    }
}
```

---

## 🔧 Cara Pasang Resourcepack

### Development (local test):
1. Build jar: `mvn package`
2. Copy folder `resourcepack/` ke `.minecraft/resourcepacks/starsmp/`
3. Aktivasi di game: **Options → Resource Packs → StarSMP**

### Server (production):
1. Zip folder `resourcepack/` menjadi `starsmp.zip`
2. Upload ke hosting (e.g. GitHub Releases, Dropbox, dll.)
3. Set URL di `server.properties`:
   ```properties
   resource-pack=https://example.com/starsmp.zip
   resource-pack-sha1=<sha1-hash>
   ```

---

## 📝 Custom Model Data Reference

| Item | CMD | Base Material |
|------|-----|---------------|
| Crown of Astral Supremacy | 300 | NETHERITE_HELMET |
| Cosmic Judgement | 301 | MACE |
| Trident of Celestial Tides | 302 | TRIDENT |
| Spear of the Falling Star | 303 | NETHERITE_SPEAR |
| Constellation Breaker | 304 | CROSSBOW |
| Starlight Oath | 305 | NETHERITE_SWORD |
| Nebula Guard | 306 | SHIELD |
| Star Essence | 200 | AMETHYST_SHARD |
| Reparation Tool | 201 | BLAZE_ROD |
| Reroll Token | 202 | COMPASS |
| Star Fragment | 203 | ECHO_SHARD |
| Stars (Aqualith–Chronovar) | 101–110 | NETHER_STAR |

---

## 🛠️ Tools yang Disarankan

| Tool | Kegunaan | Link |
|------|---------|------|
| **Blockbench** | Buat 3D model (mace, crown) | https://blockbench.net |
| **GIMP / Photoshop** | Edit texture PNG | - |
| **CIT Resewn** | Custom armor texture in-game | Modrinth |
| **MCMeta Generator** | Generate pack.mcmeta | Online tools |
| **NBT Explorer** | Debug custom item data | GitHub |
