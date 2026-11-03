package com.graceofgod.mangamoi

import androidx.compose.ui.graphics.Color

enum class HairStyle { SHORT, SPIKY, BOB, LONG, TWIN_TAILS, PONYTAIL, BUNS, CURLY, SIDE_SWEPT }
enum class Accessory { NONE, GLASSES, CAT_EARS, BOW, HEADPHONES, STAR_CLIP, HALO, HORNS }
enum class Collar { SAILOR, HOODIE, SHIRT_TIE, PLAIN }

/** Un personnage manga original, entièrement dessiné par le code (aucune image). */
data class MangaCharacter(
    val id: Int,
    val name: String,
    val hairStyle: HairStyle,
    val hair: Color,
    val eyes: Color,
    val skin: Color,
    val outfit: Color,
    val accent: Color,
    val collar: Collar,
    val accessory: Accessory,
    val bgTop: Color,
    val bgBottom: Color,
    val blush: Boolean,
    val eyeSize: Float,
)

private val NAMES = listOf(
    "Hana", "Ren", "Yuki", "Kaito", "Sora", "Aiko", "Haru", "Mei", "Riku", "Nami",
    "Kenji", "Emi", "Taro", "Rin", "Daichi", "Saki", "Hiro", "Yui", "Shun", "Koharu",
    "Akira", "Mio", "Sota", "Nanami", "Kazu", "Rika", "Toma", "Hikari", "Jun", "Ayaka",
    "Itsuki", "Momo", "Ryo", "Chika", "Yuto", "Kana", "Shin", "Natsu", "Arata", "Hina",
    "Kei", "Sana", "Minato", "Ema", "Gin", "Kokoro", "Tsubasa", "Rui", "Asahi", "Nozomi",
)

private val MINT = Color(0xFF8FD5A6)
private val SKY_HAIR = Color(0xFF7FB8F0)
private val LAVENDER = Color(0xFFC3A6F0)

private val HAIRS = listOf(
    Color(0xFF1E1B2E), Color(0xFF4A2F23), Color(0xFF7B4A2D), Color(0xFFF2C94C), Color(0xFFEDE6D6),
    Color(0xFFF59AC0), Color(0xFFC9353B), Color(0xFF7E57C2), Color(0xFF2F4C9E), Color(0xFFB8BCC8),
    MINT, Color(0xFFD9733B), LAVENDER, SKY_HAIR,
)

private val EYES = listOf(
    Color(0xFF7A4B2A), Color(0xFF3D7BE0), Color(0xFF3FA06B), Color(0xFF8E5CD9),
    Color(0xFFE0A030), Color(0xFFD94A4A), Color(0xFF7A8494), Color(0xFFE06AA8),
)

private val SKINS = listOf(
    Color(0xFFFFE3D3), Color(0xFFF8D5BE), Color(0xFFEEC2A0),
    Color(0xFFD9A27E), Color(0xFFB87D5B), Color(0xFF8D5A3E),
)

private val OUTFITS = listOf(
    Color(0xFF263A6B), Color(0xFFC2404B), Color(0xFFF2F2F2), Color(0xFF23202B), Color(0xFF2F7D5B),
    Color(0xFF5B3F9E), Color(0xFFE77FA8), Color(0xFF4F9BD9), Color(0xFFF2C94C), Color(0xFF6B7280),
)

private val ACCENTS = listOf(
    Color(0xFFE53950), Color(0xFFFFC93C), Color(0xFFFF7EB6), Color(0xFF4CB8F5), Color(0xFF9B6BFF),
)

private val BACKGROUNDS = listOf(
    Color(0xFFFFB3C7) to Color(0xFFFFE4EC),
    Color(0xFFB3D4FF) to Color(0xFFE4F0FF),
    Color(0xFFC9B3FF) to Color(0xFFEFE4FF),
    Color(0xFFB3F0D4) to Color(0xFFE4FFF2),
    Color(0xFFFFD9A8) to Color(0xFFFFF2E0),
    Color(0xFF2B2D5C) to Color(0xFF5C4B8A),
    Color(0xFFFF9AA2) to Color(0xFFFFDAC1),
    Color(0xFFA0E7E5) to Color(0xFFD9F7E8),
)

private val ACCESSORIES = listOf(
    Accessory.NONE, Accessory.GLASSES, Accessory.CAT_EARS, Accessory.NONE, Accessory.BOW,
    Accessory.HEADPHONES, Accessory.STAR_CLIP, Accessory.HALO, Accessory.HORNS,
)

/** Les 50 personnages, générés de façon déterministe (toujours les mêmes). */
val CHARACTERS: List<MangaCharacter> = NAMES.mapIndexed { i, name ->
    val style = HairStyle.entries[i % HairStyle.entries.size]
    var hair = HAIRS[(i * 5 + i / 9) % HAIRS.size]
    // Pas de couettes bleu-vert : on évite toute ressemblance avec un personnage existant
    if (style == HairStyle.TWIN_TAILS && (hair == MINT || hair == SKY_HAIR)) hair = LAVENDER
    val bg = BACKGROUNDS[(i * 5 + 2) % BACKGROUNDS.size]
    MangaCharacter(
        id = i,
        name = name,
        hairStyle = style,
        hair = hair,
        eyes = EYES[(i * 3 + 1) % EYES.size],
        skin = SKINS[(i * 7 + i / 5) % SKINS.size],
        outfit = OUTFITS[(i * 3 + i / 10) % OUTFITS.size],
        accent = ACCENTS[(i * 2 + 1) % ACCENTS.size],
        collar = Collar.entries[(i + i / 4) % Collar.entries.size],
        accessory = ACCESSORIES[(i * 7 + 3) % ACCESSORIES.size],
        bgTop = bg.first,
        bgBottom = bg.second,
        blush = i % 4 != 3,
        eyeSize = 0.92f + (i % 3) * 0.06f,
    )
}
