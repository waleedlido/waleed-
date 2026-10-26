package com.example.util

import java.util.Locale

object NotificationTextFilter {

    const val CONTENT_NOT_AVAILABLE = "المحتوى غير متاح من الإشعار"

    // Arabic dual forms
    private val ARABIC_EXACT_SUMMARIES = hashSetOf(
        "رسالتان جديدتان",
        "رسالتين جديدتين",
        "رسالتان",
        "رسالتين",
        "رسالة جديدة",
        "رسائل جديدة",
        "محتوى الإشعار مخفي",
        "محتوى مخفي",
        "إشعار جديد",
        "إشعاران جديدان",
        "new message",
        "new messages",
        "content hidden",
        "notification hidden"
    )

    // Regex for: "\d+ رسائل جديدة", "رسالتان من أحمد", "5 رسائل من 2 محادثات", etc.
    private val ARABIC_SUMMARY_PATTERN = Regex(
        pattern = """^(?:\d+\s*)?(?:رسائل|رسالة|رسالتان|رسالتين)\s*(?:جديدة|جديدتان|جديدتين)?(?:\s*من\s*.+)?$""",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    private val ARABIC_MULTI_CHAT_PATTERN = Regex(
        pattern = """^\d+\s*(?:رسائل|رسالة)\s*من\s*\d+\s*(?:محادثة|محادثات|شخص|أشخاص|جهات اتصال|دردشة|دردشات).*$""",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    // Regex for: "2 new messages", "5 messages from 2 chats", "1 new message", etc.
    private val ENGLISH_SUMMARY_PATTERN = Regex(
        pattern = """^(?:\d+\s*)?(?:new\s+)?messages?(?:\s*from\s*.+)?$""",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    private val ENGLISH_MULTI_CHAT_PATTERN = Regex(
        pattern = """^\d+\s*messages?\s*from\s*\d+\s*(?:chats?|conversations?|senders?|people).*$""",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    private val ENGLISH_UNREAD_PATTERN = Regex(
        pattern = """^\d+\s*unread\s*messages?.*$""",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    /**
     * Checks if the given text represents a notification group summary or message count
     * rather than legitimate message content.
     */
    fun isSummaryOrCounterText(text: String?): Boolean {
        if (text == null) return true
        val clean = text.trim()
        if (clean.isBlank()) return true

        val lower = clean.lowercase(Locale.ROOT)

        if (ARABIC_EXACT_SUMMARIES.contains(lower)) {
            return true
        }

        if (lower.matches(ARABIC_SUMMARY_PATTERN)) {
            return true
        }

        if (lower.matches(ARABIC_MULTI_CHAT_PATTERN)) {
            return true
        }

        if (lower.matches(ENGLISH_SUMMARY_PATTERN)) {
            return true
        }

        if (lower.matches(ENGLISH_MULTI_CHAT_PATTERN)) {
            return true
        }

        if (lower.matches(ENGLISH_UNREAD_PATTERN)) {
            return true
        }

        return false
    }

    /**
     * Validates whether a text string contains genuine user-composed message content.
     */
    fun isValidMessageContent(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        return !isSummaryOrCounterText(text)
    }
}
