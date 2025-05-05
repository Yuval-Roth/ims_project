package com.imsproject.watch.utils

class FlourPile {
    private val flourArray = arrayOf(
            Array(13) { false },
            Array(11) { false },
            Array(9) { false },
            Array(7) { false },
            Array(5) { false },
            Array(3) { false }
    )
    private val counts = arrayOf(0, 0, 0, 0, 0, 0)
    private val totalMaxCount = flourArray.sumOf { it.size }
    private var totalCount = 0

    fun get(x: Int, y: Int): Boolean {
        return flourArray[x][y]
    }

    fun isFull(): Boolean {
        return totalCount >= totalMaxCount
    }

    fun addNext() {
        var targetRow = flourArray.size - 1
        while(targetRow > 0) {
            if(counts[targetRow - 1] - counts[targetRow] > 2) {
                break
            }
            targetRow--
        }
        val rowCount = counts[targetRow]
        var targetCol = flourArray[targetRow].size / 2
        if(rowCount > 0) {
            if(rowCount % 2 == 0) {
                targetCol += (rowCount / 2)
            } else {
                targetCol -= (rowCount + 1) / 2
            }
        }
        flourArray[targetRow][targetCol] = true
        counts[targetRow]++
        totalCount++
    }
}

// Test for the class
// It visually prints the flour pile in a 2D grid format
fun main() {
    val flourPile = FlourPile()
    val lengths = arrayOf(13, 11, 9, 7, 5, 3)
    repeat(48) {
        flourPile.addNext()
        for(i in 5 downTo 0) {
            val padding = " ".repeat((13 - lengths[i]) / 2)
            print("|")
            print(padding)
            for(j in 0 until lengths[i]) {
                print(if(flourPile.get(i,j)) "+" else " ")
            }
            print(padding)
            println("|")
        }
        println("=".repeat(13 + 2))
    }
}