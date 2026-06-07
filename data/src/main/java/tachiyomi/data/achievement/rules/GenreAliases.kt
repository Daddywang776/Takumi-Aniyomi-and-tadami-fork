package tachiyomi.data.achievement.rules

object GenreAliases {

    private val genreAliases: Map<String, List<String>> = mapOf(
        "Harem" to listOf("Гарем"),
        "Isekai" to listOf("Исекай"),
        "Shounen" to listOf("Сёнэн", "Шонен"),
        "Super Power" to listOf("Суперсила", "Сверхспособности"),
        "Military" to listOf("Военное", "Военные"),
        "Psychological" to listOf("Психологическое", "Психологический"),
        "Tragedy" to listOf("Трагедия"),
        "Drama" to listOf("Драма"),
    )

    private val titleAliases: Map<String, List<String>> = mapOf(
        "jojo" to listOf("джоджо", "джо джо"),
    )

    fun allGenreSearchTerms(canonicalGenre: String): List<String> {
        return listOf(canonicalGenre) + (genreAliases[canonicalGenre].orEmpty())
    }

    fun allTitleSearchTerms(canonicalPattern: String): List<String> {
        return listOf(canonicalPattern) + (titleAliases[canonicalPattern].orEmpty())
    }

    fun genreMatches(genreEntry: String, canonicalGenres: Collection<String>): Boolean {
        return canonicalGenres.any { canonical ->
            allGenreSearchTerms(canonical).any { alias ->
                genreEntry.equals(alias, ignoreCase = true)
            }
        }
    }
}
