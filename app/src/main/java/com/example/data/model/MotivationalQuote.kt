package com.example.data.model

import java.util.Calendar

data class MotivationalQuote(
    val id: Int,
    val text: String,
    val author: String,
    val category: String = "Focus & Mastery"
)

object QuoteLibrary {
    val quotes = listOf(
        MotivationalQuote(
            id = 1,
            text = "We are what we repeatedly do. Excellence, then, is not an act, but a habit.",
            author = "Will Durant (on Aristotle)",
            category = "Habit & Consistency"
        ),
        MotivationalQuote(
            id = 2,
            text = "Deep work is the ability to focus without distraction on a cognitively demanding task.",
            author = "Cal Newport",
            category = "Deep Focus"
        ),
        MotivationalQuote(
            id = 3,
            text = "It does not matter how slowly you go as long as you do not stop.",
            author = "Confucius",
            category = "Perseverance"
        ),
        MotivationalQuote(
            id = 4,
            text = "You do not rise to the level of your goals. You fall to the level of your systems.",
            author = "James Clear",
            category = "Atomic Systems"
        ),
        MotivationalQuote(
            id = 5,
            text = "The expert in anything was once a beginner.",
            author = "Helen Hayes",
            category = "Growth Mindset"
        ),
        MotivationalQuote(
            id = 6,
            text = "Simplicity boils down to two steps: Identify the essential. Eliminate the rest.",
            author = "Leo Babauta",
            category = "Clarity"
        ),
        MotivationalQuote(
            id = 7,
            text = "Concentration is the secret of strength in politics, in war, in trade, in short in all management of human affairs.",
            author = "Ralph Waldo Emerson",
            category = "Focus"
        ),
        MotivationalQuote(
            id = 8,
            text = "One never notices what has been done; one can only see what remains to be done.",
            author = "Marie Curie",
            category = "Curiosity & Drive"
        ),
        MotivationalQuote(
            id = 9,
            text = "Small disciplines repeated with consistency every day lead to great achievements gained slowly over time.",
            author = "John C. Maxwell",
            category = "Long-Term Streaks"
        ),
        MotivationalQuote(
            id = 10,
            text = "It's not that I'm so smart, it's just that I stay with problems longer.",
            author = "Albert Einstein",
            category = "Persistence"
        ),
        MotivationalQuote(
            id = 11,
            text = "Action is the foundational key to all success.",
            author = "Pablo Picasso",
            category = "Execution"
        ),
        MotivationalQuote(
            id = 12,
            text = "You have power over your mind - not outside events. Realize this, and you will find strength.",
            author = "Marcus Aurelius",
            category = "Mindset"
        ),
        MotivationalQuote(
            id = 13,
            text = "Learning never exhausts the mind.",
            author = "Leonardo da Vinci",
            category = "Lifelong Learning"
        ),
        MotivationalQuote(
            id = 14,
            text = "The scariest moment is always just before you start.",
            author = "Stephen King",
            category = "Starting Flow"
        ),
        MotivationalQuote(
            id = 15,
            text = "Focus is a muscle. The more you practice single-tasking, the stronger your mind becomes.",
            author = "StudyFlow Insight",
            category = "Mental Stamina"
        )
    )

    fun getTodayQuote(offset: Int = 0): MotivationalQuote {
        val calendar = Calendar.getInstance()
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val index = ((dayOfYear + offset) % quotes.size + quotes.size) % quotes.size
        return quotes[index]
    }
}
