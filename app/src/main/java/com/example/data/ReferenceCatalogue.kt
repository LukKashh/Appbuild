package com.example.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream

/**
 * Bundled, immutable reference content extracted from the webapp iteration.
 * All definitions are shipped inside the APK; no remote configuration is queried.
 */
object ReferenceCatalogue {
    @Volatile private var cached: JSONObject? = null
    @Volatile private var cachedExtras: JSONObject? = null

    fun data(context: Context): JSONObject {
        cached?.let { return it }
        return synchronized(this) {
            cached ?: context.applicationContext.assets.open("capital_fortune_catalog.json.gz").use { input ->
                val output = ByteArrayOutputStream()
                GZIPInputStream(input).use { gzip -> gzip.copyTo(output) }
                JSONObject(output.toString(Charsets.UTF_8.name())).also { cached = it }
            }
        }
    }

    private fun extras(context: Context): JSONObject {
        cachedExtras?.let { return it }
        return synchronized(this) {
            cachedExtras ?: context.applicationContext.assets.open("capital_fortune_catalog_extras.json.gz").use { input ->
                val output = ByteArrayOutputStream()
                GZIPInputStream(input).use { gzip -> gzip.copyTo(output) }
                JSONObject(output.toString(Charsets.UTF_8.name())).also { cachedExtras = it }
            }
        }
    }

    fun balance(context: Context): JSONObject = data(context).getJSONObject("balance")

    fun companies(context: Context): List<JSONObject> = data(context).getJSONArray("companies").toObjects()
    fun research(context: Context): List<JSONObject> = data(context).getJSONArray("research").toObjects()
    fun properties(context: Context): List<JSONObject> = data(context).getJSONArray("properties").toObjects()
    fun propertyUpgrades(context: Context): List<JSONObject> = data(context).getJSONArray("propertyUpgrades").toObjects()
    fun corporationFacilities(context: Context): List<JSONObject> =
        data(context).getJSONObject("corporation").getJSONArray("facilities").toObjects()
    fun corporationDepartments(context: Context): List<JSONObject> =
        data(context).getJSONObject("corporation").getJSONArray("departments").toObjects()
    fun corporationBusinesses(context: Context): List<JSONObject> =
        data(context).getJSONObject("corporation").getJSONArray("businesses").toObjects()
    fun collectibleDefinitions(context: Context): List<JSONObject> =
        extras(context).getJSONObject("collectibles").getJSONArray("items").toObjects()
    fun collectibleCategories(context: Context): List<JSONObject> =
        extras(context).getJSONObject("collectibles").getJSONArray("categories").toObjects()
    fun collections(context: Context): List<JSONObject> =
        extras(context).getJSONObject("collectibles").getJSONArray("collections").toObjects()
    fun achievementDefinitions(context: Context): List<JSONObject> =
        extras(context).getJSONObject("achievements").getJSONArray("items").toObjects()

    fun company(context: Context, id: String): JSONObject? =
        companies(context).firstOrNull { it.optString("id") == id }

    fun property(context: Context, id: String): JSONObject? =
        properties(context).firstOrNull { it.optString("id") == id }

    fun researchProject(context: Context, id: String): JSONObject? =
        research(context).firstOrNull { it.optString("id") == id }

    private fun JSONArray.toObjects(): List<JSONObject> =
        (0 until length()).mapNotNull { optJSONObject(it) }
}
