
package com.example.brassistant

object AnswerEngine {

    private val capitals = mapOf(
        "франции" to "Париж",
        "германии" to "Берлин",
        "италии" to "Рим",
        "японии" to "Токио",
        "казахстана" to "Астана",
        "бразилии" to "Бразилиа",
        "россии" to "Москва",
        "китая" to "Пекин",
        "испания" to "Мадрид",
        "испаннии" to "Мадрид",
        "великобритании" to "Лондон",
        "сша" to "Вашингтон",
        "канады" to "Оттава",
        "австралии" to "Канберра",
        "турции" to "Анкара",
        "египта" to "Каир",
        "украины" to "Киев",
        "беларуси" to "Минск",
        "польши" to "Варшава",
        "индии" to "Нью-Дели"
    )

    fun findAnswer(raw: String): String {
        val text = raw.lowercase()
            .replace('×', '*')
            .replace('х', '*')
            .replace('÷', '/')

        val capital = Regex(
            "(?i)(?:столиц[аы]|столица)\\s+(?:ли)?([а-яё]+)"
        ).find(text)

        if (capital != null) {
            val country = capital.groupValues[1]
                .trim('?', '.', ',', ':')
            capitals[country]?.let {
                return "Столица: $it"
            }
        }

        val expressions = Regex(
            "(?<![\\w.])(-?\\d{1,9})\\s*([+*/-])\\s*(-?\\d{1,9})(?![\\w.])"
        )

        val match = expressions.find(text) ?: return ""

        val a = match.groupValues[1].toLongOrNull() ?: return ""
        val b = match.groupValues[3].toLongOrNull() ?: return ""

        val result = when (match.groupValues[2]) {
            "+" -> a + b
            "-" -> a - b
            "*" -> a * b
            "/" -> if (b != 0L && a % b == 0L) {
                a / b
            } else {
                return ""
            }
            else -> return ""
        }

        return "Ответ: $result"
    }
}
ы
