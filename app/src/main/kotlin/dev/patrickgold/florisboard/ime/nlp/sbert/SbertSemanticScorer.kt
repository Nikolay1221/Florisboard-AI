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
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.sqrt

class SbertSemanticScorer(private val context: Context) {
    private var interpreter: Interpreter? = null
    private var tokenizer: WordPieceTokenizer? = null
    private val maxSeqLen = 64

    fun initialize(modelPath: String, vocabPath: String) {
        try {
            tokenizer = WordPieceTokenizer(context, vocabPath)
            interpreter = Interpreter(loadModelFile(context, modelPath))
        } catch (e: Exception) {
            e.printStackTrace()
            interpreter = null
        }
    }

    private fun loadModelFile(context: Context, modelPath: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    /**
     * Scores the candidates based on how well they fit the preceding context.
     * Higher score means better semantic fit.
     */
    fun scoreCandidates(contextStr: String, candidates: List<String>): List<Pair<String, Double>> {
        if (interpreter == null || tokenizer == null || candidates.isEmpty()) {
            return candidates.map { it to 1.0 }
        }

        // Tokenize context
        val baseTokens = tokenizer!!.tokenize(contextStr, maxSeqLen)
        
        // We will compute similarity for each candidate
        val results = mutableListOf<Pair<String, Double>>()
        
        // Embed the context
        val contextEmbedding = getEmbedding(contextStr) ?: return candidates.map { it to 1.0 }
        
        for (candidate in candidates) {
            val sentence = if (contextStr.isBlank()) candidate else "$contextStr $candidate"
            val candidateEmbedding = getEmbedding(sentence)
            if (candidateEmbedding != null) {
                val similarity = cosineSimilarity(contextEmbedding, candidateEmbedding)
                results.add(candidate to similarity)
            } else {
                results.add(candidate to -1.0)
            }
        }
        
        return results.sortedByDescending { it.second }
    }

    private fun getEmbedding(text: String): FloatArray? {
        if (interpreter == null || tokenizer == null) return null
        
        val inputIds = IntArray(maxSeqLen)
        val attentionMask = IntArray(maxSeqLen)
        val tokenTypeIds = IntArray(maxSeqLen)
        
        val tokens = tokenizer!!.tokenize(text, maxSeqLen)
        for (i in 0 until maxSeqLen) {
            inputIds[i] = tokens[i]
            attentionMask[i] = if (tokens[i] != 0) 1 else 0
            tokenTypeIds[i] = 0
        }
        
        // The wrapper we exported expects batch dimension
        val inputs = arrayOf(
            arrayOf(inputIds),
            arrayOf(attentionMask),
            arrayOf(tokenTypeIds)
        )
        
        // rubert-tiny2 embedding size is 312
        val outputMap = HashMap<Int, Any>()
        val outputArr = Array(1) { FloatArray(312) }
        outputMap[0] = outputArr
        
        try {
            interpreter!!.runForMultipleInputsOutputs(inputs, outputMap)
            return outputArr[0]
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun cosineSimilarity(vec1: FloatArray, vec2: FloatArray): Double {
        var dotProduct = 0.0
        var norm1 = 0.0
        var norm2 = 0.0
        for (i in vec1.indices) {
            dotProduct += vec1[i] * vec2[i]
            norm1 += vec1[i] * vec1[i]
            norm2 += vec2[i] * vec2[i]
        }
        return if (norm1 == 0.0 || norm2 == 0.0) 0.0 else dotProduct / (sqrt(norm1) * sqrt(norm2))
    }
    
    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
