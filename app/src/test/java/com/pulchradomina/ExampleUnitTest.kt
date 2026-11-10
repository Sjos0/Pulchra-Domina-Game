package com.pulchradomina

import com.pulchradomina.data.model.GameState
import com.pulchradomina.domain.story.ChapterOneStory
import com.pulchradomina.domain.story.MangaContent
import com.pulchradomina.domain.exploration.StreetContent
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testStoryTreeIntegrity() {
        val nodes = ChapterOneStory.nodes
        assertTrue("Deve conter nós na história do Capítulo 1", nodes.isNotEmpty())

        // Verifica que todos os próximos nós referenciados existem
        nodes.values.forEach { node ->
            node.nextNodeId?.let { nextId ->
                assertTrue(
                    "O nó [${node.id}] aponta para nextNodeId [$nextId] que não existe",
                    nodes.containsKey(nextId)
                )
            }
            node.choices.forEach { choice ->
                assertTrue(
                    "A escolha [${choice.id}] no nó [${node.id}] aponta para nextNodeId [${choice.nextNodeId}] que não existe",
                    nodes.containsKey(choice.nextNodeId)
                )
            }
        }
    }

    @Test
    fun testMangaCutsceneSequence() {
        val panels = MangaContent.prologueSequence
        assertEquals(4, panels.size)
        assertTrue(panels.any { it.id == "panel_1" })
        assertTrue(panels.any { it.id == "panel_4" })
        panels.forEach { panel ->
            assertTrue(panel.title.isNotBlank())
            assertTrue(panel.narrativeText.isNotBlank())
        }
    }

    @Test
    fun testStreetExplorationHotspots() {
        val hotspots = StreetContent.streetHotspots
        assertTrue("Deve conter hotspots na rua", hotspots.isNotEmpty())
        val clubDoor = hotspots.firstOrNull { it.isClubDoor }
        assertNotNull("Deve existir hotspot para a porta da balada", clubDoor)
        assertEquals("club_entrance", clubDoor?.id)
        assertTrue("A porta do clube deve estar à direita no cenário", (clubDoor?.positionPercent ?: 0f) > 70f)
    }

    @Test
    fun testMacroRoute3aIdeal() {
        // Rota 3a: Ideal (pegou flor + alta pontuação + alta confiança)
        val state = GameState(
            pont_cap = 9,
            confianca_maveth = 5,
            pegou_flor = true,
            escolhas_feitas = listOf("aceitou_flor_com_reverencia")
        )
        val route = ChapterOneStory.calculateMacroRoute(state)
        assertEquals("3a", route.code)
    }

    @Test
    fun testMacroRoute3fSpeedrunOrZeroScore() {
        // Rota 3f: Nota mínima / fuga rápida
        val state = GameState(
            pont_cap = 0,
            confianca_maveth = 0,
            pegou_flor = false,
            escolhas_feitas = listOf("tentou_fugir")
        )
        val route = ChapterOneStory.calculateMacroRoute(state)
        assertEquals("3f", route.code)
    }

    @Test
    fun testMacroRoute3eSemFlor() {
        // Rota 3e: Recusou flor e manteve o foco na realidade
        val state = GameState(
            pont_cap = 2,
            confianca_maveth = 1,
            pegou_flor = false,
            escolhas_feitas = listOf("recusou_flor_gentil")
        )
        val route = ChapterOneStory.calculateMacroRoute(state)
        assertEquals("3e", route.code)
    }

    @Test
    fun testMacroRoute3dNoorConnection() {
        // Rota 3d: Conexão forte com Noor sem a flor
        val state = GameState(
            pont_cap = 5,
            confianca_maveth = 0,
            pegou_flor = false,
            escolhas_feitas = listOf("ouviu_noor")
        )
        val route = ChapterOneStory.calculateMacroRoute(state)
        assertEquals("3d", route.code)
    }

    @Test
    fun testMacroRoute3bMedianaComFlor() {
        // Rota 3b: Pegou a flor, pontuação mediana
        val state = GameState(
            pont_cap = 4,
            confianca_maveth = 2,
            pegou_flor = true,
            escolhas_feitas = listOf("aceitou_flor_hesitante")
        )
        val route = ChapterOneStory.calculateMacroRoute(state)
        assertEquals("3b", route.code)
    }

    @Test
    fun testMacroRoute3cCuriosidadeMavethSemFlor() {
        // Rota 3c: Não pegou a flor, mas teve afinidade com Maveth
        val state = GameState(
            pont_cap = 4,
            confianca_maveth = 3,
            pegou_flor = false,
            escolhas_feitas = listOf("tom_curioso", "recusou_flor_gentil")
        )
        val route = ChapterOneStory.calculateMacroRoute(state)
        assertEquals("3c", route.code)
    }
}
