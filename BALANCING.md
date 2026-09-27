# ⚖️ StarSMP: Balancing & Combat Design

Dokumen ini berfungsi sebagai panduan resmi untuk penyeimbangan mekanik, kemampuan Star, dan item legendaris di StarSMP. Fokus utama kami adalah menciptakan ekosistem PvP yang kompetitif, adil, dan berbasis skill.

---

## 🎯 Filosofi Balancing
Kami menggunakan tiga pilar utama dalam menentukan setiap perubahan:
1.  **Risk vs. Reward:** Kemampuan yang sangat kuat harus memiliki risiko tinggi atau biaya yang besar.
2.  **Counter-play:** Tidak boleh ada kemampuan "Instant-Win". Setiap serangan harus memiliki cara untuk dihindari atau ditangkis.
3.  **Role Identity:** Setiap Star harus terasa unik dan memiliki peran spesifik (Tank, DPS, Utility, Stealth).

---

## 🛠️ Mekanik Global (Core Balance)

### 1. Shield Interaction
*   **Guard Break:** Serangan berat (seperti *Stone Smash* atau *Inferno Surge*) akan menonaktifkan Shield musuh selama **3.5 detik**.
*   **Shield Decay:** Menggunakan shield terus-menerus saat terkena efek *Wither* atau *Bleed* akan mempercepat kerusakan shield (Durability).

### 2. Combat Tag & Safety
*   **Anti-Escape:** Pemain yang sedang dalam status *Combat Tag* (15 detik setelah memukul/dipukul) tidak dapat menggunakan `/spawn`, `/home`, atau memutus koneksi tanpa risiko penalti *Star Shattering*.
*   **Verticality Cap:** Kemampuan terbang atau *levitation* akan dibatasi pada ketinggian **Y=255** untuk mencegah pemain "camping" di langit.

### 3. Spawn Protection (Death Grace Period)
*   **Safety Window:** Setelah mati, pemain mendapatkan status *Spawn Protection* selama **15 menit** (Dapat dikonfigurasi).
*   **Anti-Spawn Kill:** Pemain dengan status ini tidak dapat diserang oleh pemain lain, namun tetap bisa terkena damage dari lingkungan (Lava, Fall Damage, Mobs).
*   **Automatic Expiry:** Status perlindungan akan langsung **hilang** jika pemain melakukan aksi berikut:
    *   Menyerang pemain lain atau mob.
    *   Menggunakan *Active Ability* Star.
    *   Mengambil item legendaris atau *Star Essence*.

---

## 🌟 Penyesuaian Spesifik Star (Proposals)

| Star | Status | Penyesuaian | Alasan |
| :--- | :--- | :--- | :--- |
| **🌊 Aqualith** | Buff | Efek "Tidal Grace" bertahan 5 detik setelah keluar air. | Memberi ruang gerak lebih luas di darat. |
| **🔥 Pyrelion** | **Buff** | **Lava Navigator:** Dapat berenang cepat di Lava (Lava Swimming). | Mempermudah travel di Nether & kontrol area panas. |
| **🌪 Zephyros** | Adjust | Knockback diterima bertambah 25%. | Mengimbangi mobilitas tinggi agar gampang diusir. |
| **🪨 Gravaryn** | **Nerf** | Health Boost III dikurangi menjadi **Health Boost II (+4 Hearts)**. | Terlalu tanky, hampir mustahil dibunuh di Tier 3. |
| **⚡ Voltyra** | Adjust | Charge berkurang jika tidak menyerang dalam 10 detik. | Mencegah pemain menyimpan "Stun" selamanya. |
| **🌿 Verdaryn** | **Buff** | Restorasi hunger dipercepat (1.5s) & durasi Absorption II ditambah. | Memberikan sustain yang lebih konsisten di tengah war. |
| **🌑 Umbryx** | Adjust | Sprinting memunculkan partikel asap di kaki. | Memberikan celah bagi musuh untuk mendeteksi lokasi. |
| **✨ Luminary** | **Nerf** | Damage *Radiant Pulse* dikurangi 20% & durasi blind dipangkas. | Damage dasarnya sudah terlalu OP. |
| **🌌 Aetherion** | Nerf | Bonus Crit Damage dibatasi +80% untuk PvP. | Mencegah "One-Shot" build yang tidak sehat. |
| **⏳ Chronovar** | Adjust | Muncul bunyi "Ticking" sebelum Time Stop aktif. | Memberi tanda peringatan (Telegraphing) bagi musuh. |

---

## ⚙️ System Stability (Bug Fixes)
*   **Health Boost Persistence:** Durasi efek Health Boost dari passive ditingkatkan dari 1-2 detik menjadi **15 detik** dengan sistem refresh otomatis. Ini mencegah "HP Flickering" atau bug bar darah yang menghilang tiba-tiba.
*   **Attribute Sync:** Sinkronisasi atribut (Health/Speed) dilakukan setiap kali pemain mengganti item di hotbar untuk memastikan stats selalu akurat.

---

## ⚔️ Legendary Artifacts

### 🔨 Cosmic Judgement (Mace)
*   **Wind Charge Scaling:** Bonus damage dari jatuh dibatasi pada ketinggian 30 blok untuk mencegah damage yang tidak masuk akal.
*   **Cooldown:** Cooldown antar *Smash* ditingkatkan menjadi 45 detik jika digunakan oleh Star dengan *Strength passive*.

### 🗡️ Spear of the Falling Star
*   **Bleed Stacking:** Efek bleed tidak bisa menumpuk (stack) lebih dari 3 kali.
*   **Speed Cap:** Maksimal speed dari spear adalah Speed IV, tidak bisa digabung dengan potion Speed.

### 🛡️ Starlight Oath (Shield)
*   **Nerf:** Durasi penonaktifan shield musuh dikurangi dari 5 detik menjadi **3 detik**.
*   **Cooldown:** Cooldown ability meningkat 15% untuk mencegah stun-lock berkelanjutan.

---

## 🗺️ Biome Affinity (Kekuatan Alam)
Setiap Star memiliki keterikatan dengan lingkungan tertentu. Berada di biome yang tepat akan memberikan bonus statistik tambahan.

| Star | Biome Favorit | Efek Bonus |
| :--- | :--- | :--- |
| **🌊 Aqualith** | Ocean, River, Rain | +10% Damage & Regen HP sedikit lebih cepat. |
| **🔥 Pyrelion** | Desert, Nether, Savanna | +10% Fire Damage & Resistance tambahan. |
| **🌪 Zephyros** | Mountains, Y > 120 | +15% Movement Speed & Jump Boost I. |
| **🪨 Gravaryn** | Caves, Deepslate (Y < 0) | +50% Knockback Resistance & Resistance I. |
| **⚡ Voltyra** | Thunderstorm | Charge listrik terisi 2x lebih cepat. |
| **🌿 Verdaryn** | Jungle, Forest, Lush Caves | Regeneration I (Passive) & +2 Max HP. |
| **🌑 Umbryx** | Deep Dark, End, Y < 0 | Invisibility tidak memunculkan partikel sama sekali. |
| **✨ Luminary** | Day Time, Cherry Grove | Strength I (Permanent selama di lokasi/waktu tsb). |
| **🌌 Aetherion** | The End | Radius *Gravity Well* meningkat 20%. |
| **⏳ Chronovar** | Ancient City, End Barrens | Cooldown Ability 10% lebih cepat. |

---

## 📈 Rencana Pengembangan (Roadmap)
1.  **Phase 1:** Implementasi "Visual Telegraphing" untuk semua kemampuan Tier 3.
2.  **Phase 2:** Sistem Spawn Protection (Grace Period) per pemain setelah mati.
3.  **Phase 3:** Anti-Kill-Farming (Cooldown per-target untuk mendapatkan Essence).

---

*Note: Dokumentasi ini akan terus diperbarui seiring dengan perkembangan plugin.*
