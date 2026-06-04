/*
 * Copyright (C) 2025 The FlorisBoard Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.patrickgold.florisboard.ime.nlp.sbert

import android.content.Context
import org.florisboard.lib.android.readText
import java.util.Locale

class WordPieceTokenizer(context: Context, vocabPath: String) {
    private val vocab = mutableMapOf<String, Int>()
    private val unkToken = "[UNK]"
    private val unkTokenId: Int
    private val clsTokenId: Int
    private val sepTokenId: Int
    private val padTokenId: Int
    
    init {
        val lines = context.assets.readText(vocabPath).lines()
        for ((index, line) in lines.withIndex()) {
            val token = line.trim()
            if (token.isNotEmpty()) {
                vocab[token] = index
            }
        }
        unkTokenId = vocab[unkToken] ?: 1
        clsTokenId = vocab["[CLS]"] ?: 2
        sepTokenId = vocab["[SEP]"] ?: 3
        padTokenId = vocab["[PAD]"] ?: 0
    }

    /**
     * Tokenizes a text into a list of token IDs.
     * Optionally pads or truncates to maxLen.
     */
    fun tokenize(text: String, maxLen: Int = 64): IntArray {
        val tokens = mutableListOf<Int>()
        tokens.add(clsTokenId)
        
        // Basic whitespace tokenization and lowercase (for uncased models)
        val words = text.lowercase(Locale.getDefault()).split("\\s+".toRegex()).filter { it.isNotEmpty() }
        
        for (word in words) {
            val wordTokens = wordpieceTokenize(word)
            for (token in wordTokens) {
                if (tokens.size < maxLen - 1) { // leave room for [SEP]
                    tokens.add(token)
                }
            }
        }
        
        if (tokens.size < maxLen) {
            tokens.add(sepTokenId)
        } else {
            tokens[maxLen - 1] = sepTokenId
        }
        
        val result = IntArray(maxLen) { padTokenId }
        for (i in tokens.indices) {
            result[i] = tokens[i]
        }
        
        return result
    }

    private fun wordpieceTokenize(word: String): List<Int> {
        val outputTokens = mutableListOf<Int>()
        var isBad = false
        var start = 0
        val maxInputCharsPerWord = 200
        
        if (word.length > maxInputCharsPerWord) {
            outputTokens.add(unkTokenId)
            return outputTokens
        }
        
        while (start < word.length) {
            var end = word.length
            var curSubStr: String? = null
            while (start < end) {
                var substr = word.substring(start, end)
                if (start > 0) {
                    substr = "##$substr"
                }
                if (vocab.containsKey(substr)) {
                    curSubStr = substr
                    break
                }
                end -= 1
            }
            if (curSubStr == null) {
                isBad = true
                break
            }
            vocab[curSubStr]?.let { outputTokens.add(it) }
            start = end
        }
        
        if (isBad) {
            return listOf(unkTokenId)
        }
        return outputTokens
    }
}
