package dev.pocketdev.app.ai

import dev.pocketdev.app.core.Endpoints
import dev.pocketdev.app.core.Http
import dev.pocketdev.app.core.InferenceLocation
import dev.pocketdev.app.core.ProviderKind
import org.json.JSONObject

data class ProviderCheck(val reachable: Boolean, val detail: String, val models: List<String> = emptyList())

/** Provider abstraction. Milestone 2 adds chat/tool-calling; v1 only verifies connectivity. */
interface AiProvider {
    val kind: ProviderKind
    val baseUrl: String
    val location: InferenceLocation get() = Endpoints.inferenceLocation(baseUrl)
    suspend fun check(): ProviderCheck
}

class OllamaProvider(override val baseUrl: String) : AiProvider {
    override val kind = ProviderKind.OLLAMA

    override suspend fun check(): ProviderCheck = try {
        val r = Http.get(baseUrl.trimEnd('/') + "/api/tags")
        if (r.code in 200..299) {
            val arr = JSONObject(r.body).optJSONArray("models")
            val names = buildList { if (arr != null) for (i in 0 until arr.length()) add(arr.getJSONObject(i).optString("name")) }
            ProviderCheck(true, "Ollama answered with ${names.size} installed model(s)", names)
        } else {
            ProviderCheck(false, "Ollama endpoint answered HTTP ${r.code}")
        }
    } catch (e: Exception) {
        ProviderCheck(false, "No answer from $baseUrl (${e.javaClass.simpleName}). Is Ollama running?")
    }
}

class OpenAiCompatibleProvider(override val baseUrl: String, private val apiKey: String? = null) : AiProvider {
    override val kind = ProviderKind.OPENAI_COMPATIBLE

    override suspend fun check(): ProviderCheck = try {
        val r = Http.get(baseUrl.trimEnd('/') + "/models", bearer = apiKey)
        when {
            r.code in 200..299 -> {
                val arr = JSONObject(r.body).optJSONArray("data")
                val ids = buildList { if (arr != null) for (i in 0 until arr.length()) add(arr.getJSONObject(i).optString("id")) }
                ProviderCheck(true, "Endpoint listed ${ids.size} model(s)", ids)
            }
            r.code == 401 || r.code == 403 ->
                ProviderCheck(true, "Endpoint is reachable but requires a valid API key (HTTP ${r.code}). Key storage arrives in the next milestone.")
            else -> ProviderCheck(false, "Endpoint answered HTTP ${r.code}")
        }
    } catch (e: Exception) {
        ProviderCheck(false, "No answer from $baseUrl (${e.javaClass.simpleName})")
    }
}

object ProviderFactory {
    fun create(kind: ProviderKind, baseUrl: String): AiProvider = when (kind) {
        ProviderKind.OLLAMA -> OllamaProvider(baseUrl)
        ProviderKind.OPENAI_COMPATIBLE -> OpenAiCompatibleProvider(baseUrl)
    }
}

data class AgentVerification(val verified: Boolean, val detail: String)

/** Placeholder seam for the coding agent (milestone 2). Nothing is claimed as verified until it is tested. */
interface CodingAgent {
    val name: String
    suspend fun verify(provider: AiProvider): AgentVerification
}

object NoCodingAgent : CodingAgent {
    override val name = "None configured"
    override suspend fun verify(provider: AiProvider) =
        AgentVerification(false, "No coding agent is installed yet. Agent support arrives in the next milestone.")
}
