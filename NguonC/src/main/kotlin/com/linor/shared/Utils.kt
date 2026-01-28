package com.linor.shared

object Utils {
    fun countWords(text: String?): Int {
        if (text == null) return 0
        return text.trim().split("\\s+".toRegex()).size
    }

    fun formatImdb(score: Any?, useFiveStars: Boolean): String {
        return score?.toString() ?: ""
    }

    fun formatYear(year: Int?): String {
        return if (year != null) " ($year)" else ""
    }
}
