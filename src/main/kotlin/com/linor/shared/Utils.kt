package com.linor.shared

object Utils {
    fun countWords(text: String): Int {
        return text.trim().split("\\s+".toRegex()).size
    }

    fun formatImdb(score: Double?, useFiveStars: Boolean): String {
        return score?.toString() ?: ""
    }
}
