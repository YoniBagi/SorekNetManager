package com.bagi.soreknetmanager.helpers

import java.util.regex.Matcher
import java.util.regex.Pattern

object ConvertStringToHtml {
    fun txtToHtml(originalString: String?): String? {
        if (originalString == null){
            return null
        }
        val builder = StringBuilder()
        var previousWasASpace = false
        for ((i,c) in originalString.toCharArray().withIndex()) {
            if (c == ' ') {
                if (previousWasASpace) {
                    builder.append("&nbsp;")
                    previousWasASpace = false
                    continue
                }
                previousWasASpace = true
            } else {
                previousWasASpace = false
            }
            when (c) {
                '<' -> builder.append("&lt;")
                '>' -> builder.append("&gt;")
                '&' -> builder.append("&amp;")
                '"' -> builder.append("&quot;")
                '\n' -> builder.append("<br>")
                '\t' -> builder.append("&nbsp; &nbsp; &nbsp;")
                else -> builder.append(c)
            }
        }
        var converted = builder.toString()
        val str =
            "(?i)\\b((?:https?://|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,4}/)(?:[^\\s()<>]+|\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\))+(?:\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\)|[^\\s`!()\\[\\]{};:\'\".,<>?«»“”‘’]))"
        val patt= Pattern.compile(str)
        val matcher= patt.matcher(converted)
        converted = matcher.replaceAll("<a href=\"$1\">$1</a>")
        return converted
    }
}