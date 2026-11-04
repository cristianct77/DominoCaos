package com.udistrital.dominocaos.logic

import com.udistrital.dominocaos.model.BoardTile
import com.udistrital.dominocaos.model.Domino
import com.udistrital.dominocaos.model.Side
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class DominoRulesTest {

    @Test
    fun fullDeck_has28UniqueTiles() {
        val deck = DominoRules.fullDeck()
        assertEquals(28, deck.size)
        assertEquals(28, deck.map { it.key }.toSet().size)
    }

    @Test
    fun deal_gives7TilesToEachPlayer_andRestToPool() {
        val (hands, pool) = DominoRules.deal(2, Random(1))
        assertEquals(2, hands.size)
        hands.forEach { assertEquals(DominoRules.TILES_PER_PLAYER, it.size) }
        assertEquals(14, pool.size)
        val all = hands.flatten() + pool
        assertEquals(28, all.map { it.key }.toSet().size)
    }

    @Test
    fun anyTile_canBePlacedOnEmptyBoard() {
        assertTrue(DominoRules.canPlace(Domino(2, 5), emptyList(), Side.LEFT))
        assertTrue(DominoRules.canPlace(Domino(2, 5), emptyList(), Side.RIGHT))
    }

    @Test
    fun tile_mustMatchTheBoardEnd() {
        val board = listOf(BoardTile(Domino(3, 6)))
        assertTrue(DominoRules.canPlace(Domino(1, 3), board, Side.LEFT))
        assertFalse(DominoRules.canPlace(Domino(1, 3), board, Side.RIGHT))
        assertTrue(DominoRules.canPlace(Domino(6, 6), board, Side.RIGHT))
        assertTrue(DominoRules.validSides(Domino(2, 4), board).isEmpty())
    }

    @Test
    fun place_orientsTileToMatchEnds() {
        var board = DominoRules.place(emptyList(), Domino(3, 6), Side.RIGHT)
        board = DominoRules.place(board, Domino(1, 3), Side.LEFT)   // queda 1|3
        board = DominoRules.place(board, Domino(4, 6), Side.RIGHT)  // queda 6|4
        assertEquals(Domino(1, 3), board.first().domino)
        assertEquals(Domino(6, 4), board.last().domino)
        assertEquals(1, board.first().domino.left)
        assertEquals(4, board.last().domino.right)
    }

    @Test
    fun hasAnyMove_detectsBlockedHand() {
        val board = listOf(BoardTile(Domino(6, 6)))
        assertFalse(DominoRules.hasAnyMove(listOf(Domino(0, 1), Domino(2, 3)), board))
        assertTrue(DominoRules.hasAnyMove(listOf(Domino(0, 1), Domino(2, 6)), board))
    }

    @Test
    fun domino_keyIgnoresOrientation() {
        assertTrue(Domino(2, 5).sameAs(Domino(5, 2)))
        assertEquals(Domino(2, 5), Domino.fromKey("2|5"))
    }
}
