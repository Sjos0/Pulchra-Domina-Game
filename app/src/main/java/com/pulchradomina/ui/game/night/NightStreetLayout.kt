// GERADO por tools/night_street/build_night_street_assets.py — não editar à mão.
package com.pulchradomina.ui.game.night

/** Sprites do cenário noturno (arquivos em app/src/main/assets/night_street/). */
enum class NightSprite(val file: String) {
    MOON("moon.png"),
    CLOUD_1("cloud_1.png"),
    CLOUD_2("cloud_2.png"),
    CLOUD_3("cloud_3.png"),
    CLOUD_4("cloud_4.png"),
    CITY_1("city_1.png"),
    CITY_2("city_2.png"),
    CITY_3("city_3.png"),
    CITY_4("city_4.png"),
    TREE_1("tree_1.png"),
    TREE_2("tree_2.png"),
    TREE_3("tree_3.png"),
    TREE_4("tree_4.png"),
    TREE_5("tree_5.png"),
    TREE_6("tree_6.png"),
    BUSH_1("bush_1.png"),
    BUSH_2("bush_2.png"),
    BENCH_1("bench_1.png"),
    BENCH_2("bench_2.png"),
    LAMP("lamp.png"),
    SIDEWALK("sidewalk.png"),
    ROAD("road.png"),
    ITEM("item_maveth_flower.png"),
    ITEM_INDICATOR("item_maveth_indicator.png"),
    LAMP_LIGHT("lamp_light.png"),
    CLUB_SPILL("club_spill.png"),
    CLUB_ACCENTS("club_accents.png"),
    RIM_TREE_1_R("rim_tree_1_r.png"),
    RIM_TREE_1_L("rim_tree_1_l.png"),
    RIM_TREE_2_R("rim_tree_2_r.png"),
    RIM_TREE_2_L("rim_tree_2_l.png"),
    RIM_TREE_3_R("rim_tree_3_r.png"),
    RIM_TREE_3_L("rim_tree_3_l.png"),
    RIM_TREE_4_R("rim_tree_4_r.png"),
    RIM_TREE_4_L("rim_tree_4_l.png"),
    RIM_TREE_5_R("rim_tree_5_r.png"),
    RIM_TREE_5_L("rim_tree_5_l.png"),
    RIM_TREE_6_R("rim_tree_6_r.png"),
    RIM_TREE_6_L("rim_tree_6_l.png"),
    RIM_BENCH_1_R("rim_bench_1_r.png"),
    RIM_BENCH_1_L("rim_bench_1_l.png"),
    RIM_BENCH_2_R("rim_bench_2_r.png"),
    RIM_BENCH_2_L("rim_bench_2_l.png");
}

/** Camadas de fundo -> frente. A calçada, a rua, os postes e o Edgar ficam fora destas camadas. */
enum class Layer { SKY, FAR, TREE, BUSH, FRONT }

/**
 * Prop no mundo, em unidades de referência (941 x 1672).
 * [x] centro horizontal, [top] topo, [h] altura. [parallax] 1f acompanha a câmera.
 * [drift] deslocamento lento em unidades por segundo (nuvens).
 */
data class Prop(
    val sprite: NightSprite,
    val x: Float,
    val top: Float,
    val h: Float,
    val layer: Layer,
    val parallax: Float,
    val flip: Boolean,
    val drift: Float
)

/**
 * Poste. [apexY] = lâmpada (ápice do cone); [groundY] = chão onde o cone termina.
 * [topHalf]/[baseHalf] = meia largura do cone no ápice e no chão.
 */
data class Lamp(
    val x: Float,
    val baseY: Float,
    val h: Float,
    val apexY: Float,
    val groundY: Float,
    val topHalf: Float,
    val baseHalf: Float,
    val bulbW: Float,
    val bulbH: Float
)

internal object NightStreetLayout {
    const val REF_WIDTH = 941f
    const val REF_HEIGHT = 1672f
    const val BLOCK_COUNT = 3
    const val WORLD_WIDTH = 2823f
    const val GROUND_TOP = 1212f
    const val SIDEWALK_HEIGHT = 120f
    const val SIDEWALK_TILE_WIDTH = 749.0f
    const val ROAD_TOP = 1332f
    const val ROAD_TILE_HEIGHT = 234f
    const val EDGAR_HEIGHT = 330f
    const val EDGAR_FEET_Y = 1248f
    const val EDGAR_ASPECT = 0.3f
    const val EDGAR_FRAME_COUNT = 67
    const val EDGAR_FPS = 35
    const val BULB_COLOR_ARGB = 0xFFFFE6B4
    const val NIGHT_OVERLAY_ALPHA = 0.3f
    const val NIGHT_OVERLAY_ARGB = 0xFF07041A
    /** Escurecimento multiplicativo por camada de fundo (cor ARGB para ColorFilter.tint). */
    const val FAR_TINT_ARGB = 0xFF808080
    const val TREE_TINT_ARGB = 0xFF949494
    const val BUSH_TINT_ARGB = 0xFF9E9E9E
    const val FRONT_TINT_ARGB = 0xFFBDBDBD

    val props: List<Prop> = listOf(
        Prop(NightSprite.MOON, x = 677.0f, top = 190.0f, h = 126.0f, layer = Layer.SKY, parallax = 0.03f, flip = false, drift = 0.0f),
        Prop(NightSprite.CLOUD_1, x = 200.0f, top = 165.0f, h = 150.0f, layer = Layer.SKY, parallax = 0.18f, flip = false, drift = 3.0f),
        Prop(NightSprite.CLOUD_2, x = 830.0f, top = 44.0f, h = 132.0f, layer = Layer.SKY, parallax = 0.18f, flip = false, drift = 3.4f),
        Prop(NightSprite.CLOUD_3, x = 640.0f, top = 210.0f, h = 150.0f, layer = Layer.SKY, parallax = 0.18f, flip = false, drift = 2.6f),
        Prop(NightSprite.CLOUD_4, x = 880.0f, top = 355.0f, h = 110.0f, layer = Layer.SKY, parallax = 0.18f, flip = false, drift = 3.8f),
        Prop(NightSprite.CITY_1, x = 620.0f, top = 652.0f, h = 560.0f, layer = Layer.FAR, parallax = 0.5f, flip = false, drift = 0.0f),
        Prop(NightSprite.CITY_2, x = 170.0f, top = 782.0f, h = 430.0f, layer = Layer.FAR, parallax = 0.5f, flip = false, drift = 0.0f),
        Prop(NightSprite.TREE_1, x = 150.0f, top = 478.0f, h = 740.0f, layer = Layer.TREE, parallax = 1.0f, flip = false, drift = 0.0f),
        Prop(NightSprite.TREE_2, x = 790.0f, top = 658.0f, h = 560.0f, layer = Layer.TREE, parallax = 1.0f, flip = false, drift = 0.0f),
        Prop(NightSprite.BUSH_1, x = 390.0f, top = 1128.0f, h = 88.0f, layer = Layer.BUSH, parallax = 1.0f, flip = false, drift = 0.0f),
        Prop(NightSprite.BENCH_1, x = 178.0f, top = 1142.0f, h = 72.0f, layer = Layer.FRONT, parallax = 1.0f, flip = false, drift = 0.0f),
        Prop(NightSprite.BENCH_2, x = 720.0f, top = 1142.0f, h = 72.0f, layer = Layer.FRONT, parallax = 1.0f, flip = false, drift = 0.0f),
        Prop(NightSprite.CLOUD_3, x = 1161.0f, top = 185.0f, h = 140.0f, layer = Layer.SKY, parallax = 0.18f, flip = true, drift = 3.0f),
        Prop(NightSprite.CLOUD_4, x = 1641.0f, top = 90.0f, h = 120.0f, layer = Layer.SKY, parallax = 0.18f, flip = false, drift = 2.2f),
        Prop(NightSprite.CLOUD_1, x = 1801.0f, top = 280.0f, h = 120.0f, layer = Layer.SKY, parallax = 0.18f, flip = true, drift = 3.2f),
        Prop(NightSprite.CITY_3, x = 1501.0f, top = 712.0f, h = 500.0f, layer = Layer.FAR, parallax = 0.5f, flip = false, drift = 0.0f),
        Prop(NightSprite.CITY_4, x = 1071.0f, top = 752.0f, h = 460.0f, layer = Layer.FAR, parallax = 0.5f, flip = false, drift = 0.0f),
        Prop(NightSprite.TREE_3, x = 1101.0f, top = 518.0f, h = 700.0f, layer = Layer.TREE, parallax = 1.0f, flip = false, drift = 0.0f),
        Prop(NightSprite.TREE_4, x = 1741.0f, top = 698.0f, h = 520.0f, layer = Layer.TREE, parallax = 1.0f, flip = false, drift = 0.0f),
        Prop(NightSprite.BUSH_2, x = 1341.0f, top = 1132.0f, h = 84.0f, layer = Layer.BUSH, parallax = 1.0f, flip = false, drift = 0.0f),
        Prop(NightSprite.BENCH_2, x = 1119.0f, top = 1142.0f, h = 72.0f, layer = Layer.FRONT, parallax = 1.0f, flip = false, drift = 0.0f),
        Prop(NightSprite.BENCH_1, x = 1661.0f, top = 1142.0f, h = 72.0f, layer = Layer.FRONT, parallax = 1.0f, flip = true, drift = 0.0f),
        Prop(NightSprite.CLOUD_2, x = 2142.0f, top = 135.0f, h = 130.0f, layer = Layer.SKY, parallax = 0.18f, flip = false, drift = 2.8f),
        Prop(NightSprite.CLOUD_1, x = 2522.0f, top = 250.0f, h = 140.0f, layer = Layer.SKY, parallax = 0.18f, flip = true, drift = 3.6f),
        Prop(NightSprite.CITY_1, x = 2482.0f, top = 692.0f, h = 520.0f, layer = Layer.FAR, parallax = 0.5f, flip = true, drift = 0.0f),
        Prop(NightSprite.CITY_2, x = 2072.0f, top = 792.0f, h = 420.0f, layer = Layer.FAR, parallax = 0.5f, flip = true, drift = 0.0f),
        Prop(NightSprite.TREE_5, x = 2042.0f, top = 498.0f, h = 720.0f, layer = Layer.TREE, parallax = 1.0f, flip = false, drift = 0.0f),
        Prop(NightSprite.TREE_6, x = 2662.0f, top = 678.0f, h = 540.0f, layer = Layer.TREE, parallax = 1.0f, flip = true, drift = 0.0f),
        Prop(NightSprite.BUSH_1, x = 2282.0f, top = 1128.0f, h = 88.0f, layer = Layer.BUSH, parallax = 1.0f, flip = true, drift = 0.0f),
        Prop(NightSprite.BENCH_1, x = 2060.0f, top = 1142.0f, h = 72.0f, layer = Layer.FRONT, parallax = 1.0f, flip = false, drift = 0.0f),
        Prop(NightSprite.BENCH_2, x = 2602.0f, top = 1142.0f, h = 72.0f, layer = Layer.FRONT, parallax = 1.0f, flip = false, drift = 0.0f),
    )

    /** Itens no chão da calçada (desenhados depois do chão, antes dos postes e do Edgar). Camada não usada. */
    val groundProps: List<Prop> = listOf(
        Prop(NightSprite.ITEM, x = 2440.0f, top = 1199.0f, h = 53.0f, layer = Layer.FRONT, parallax = 1.0f, flip = false, drift = 0.0f),
    )

    val lamps: List<Lamp> = listOf(
        Lamp(x = 266.0f, baseY = 1222.0f, h = 455.0f, apexY = 804.92f, groundY = 1248.0f, topHalf = 22.75f, baseHalf = 172.9f, bulbW = 50.05f, bulbH = 15.93f),
        Lamp(x = 1207.0f, baseY = 1222.0f, h = 455.0f, apexY = 804.92f, groundY = 1248.0f, topHalf = 22.75f, baseHalf = 172.9f, bulbW = 50.05f, bulbH = 15.93f),
        Lamp(x = 2148.0f, baseY = 1222.0f, h = 455.0f, apexY = 804.92f, groundY = 1248.0f, topHalf = 22.75f, baseHalf = 172.9f, bulbW = 50.05f, bulbH = 15.93f),
    )

    /** Máscara de borda (luz da balada) por sprite: (bordas direitas, bordas esquerdas espelhadas). */
    val rimMasks: Map<NightSprite, Pair<NightSprite, NightSprite>> = mapOf(
        NightSprite.TREE_1 to (NightSprite.RIM_TREE_1_R to NightSprite.RIM_TREE_1_L),
        NightSprite.TREE_2 to (NightSprite.RIM_TREE_2_R to NightSprite.RIM_TREE_2_L),
        NightSprite.TREE_3 to (NightSprite.RIM_TREE_3_R to NightSprite.RIM_TREE_3_L),
        NightSprite.TREE_4 to (NightSprite.RIM_TREE_4_R to NightSprite.RIM_TREE_4_L),
        NightSprite.TREE_5 to (NightSprite.RIM_TREE_5_R to NightSprite.RIM_TREE_5_L),
        NightSprite.TREE_6 to (NightSprite.RIM_TREE_6_R to NightSprite.RIM_TREE_6_L),
        NightSprite.BENCH_1 to (NightSprite.RIM_BENCH_1_R to NightSprite.RIM_BENCH_1_L),
        NightSprite.BENCH_2 to (NightSprite.RIM_BENCH_2_R to NightSprite.RIM_BENCH_2_L),
    )

    /** Luz da balada no chão: tamanho em unidades e altura do topo dos pontos de neon. */
    const val CLUB_SPILL_W = 560.0f
    const val CLUB_SPILL_H = 420.0f
    const val CLUB_ACCENT_W = 48f
    const val CLUB_ACCENT_H = 400f
    const val CLUB_ACCENT_TOP = 620f
    const val CLUB_ACCENT_MARGIN = 10f

    /** Ícone de mão do aviso de item (SVG, viewBox 0 0 24 24), de tools/night_street/sources/hand_hint.svg. */
    const val ITEM_HINT_HAND_PATH = "M9 11.24V7.5C9 6.12 10.12 5 11.5 5S14 6.12 14 7.5v3.74c1.21-.81 2-2.18 2-3.74C16 5.01 13.99 3 11.5 3S7 5.01 7 7.5c0 1.56.79 2.93 2 3.74zm9.84 4.63l-4.54-2.26c-.17-.07-.35-.11-.54-.11H13v-6c0-.83-.67-1.5-1.5-1.5S10 6.67 10 7.5v10.74l-3.43-.72c-.08-.01-.15-.03-.24-.03-.31 0-.59.13-.79.33l-.79.8 4.94 4.94c.27.27.65.44 1.06.44h6.79c.75 0 1.33-.55 1.44-1.28l.75-5.27c.01-.07.02-.14.02-.2 0-.62-.38-1.16-.91-1.38z"
}
