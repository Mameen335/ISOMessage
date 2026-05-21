package com.mameen.isomessage.util

import android.content.Context
import com.google.gson.Gson
import com.mameen.isomessage.data.model.MockoonEnvironment
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility for loading files from the Android assets folder.
 *
 * Assets are bundled into the APK and accessed at runtime via [Context.assets].
 * This is ideal for configuration files, sample data, and JSON templates
 * that are known at build time and don't change at runtime.
 *
 * The Mockoon environment JSON lives at:
 *   app/src/main/assets/ISO8583_POS_Payment_Host.json
 */
@Singleton
class AssetLoader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()

    /**
     * Read a file from assets and return its contents as a String.
     *
     * @param fileName The filename relative to the assets root (e.g., "ISO8583_POS_Payment_Host.json")
     * @return File contents, or null if the file doesn't exist or can't be read
     */
    fun readAssetFile(fileName: String): String? = try {
        context.assets.open(fileName).bufferedReader().use { it.readText() }
    } catch (e: Exception) {
        null
    }

    /**
     * Parse the Mockoon environment JSON file into a [MockoonEnvironment] object.
     * Used by the Developer Tools screen.
     */
    fun loadMockoonEnvironment(fileName: String = "ISO8583_POS_Payment_Host.json"): MockoonEnvironment? {
        val json = readAssetFile(fileName) ?: return null
        return try {
            gson.fromJson(json, MockoonEnvironment::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * List all files in the assets root directory.
     */
    fun listAssetFiles(): List<String> = try {
        context.assets.list("")?.toList() ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }

    /**
     * Check if an asset file exists.
     */
    fun assetExists(fileName: String): Boolean = try {
        context.assets.open(fileName).close()
        true
    } catch (e: Exception) {
        false
    }
}
