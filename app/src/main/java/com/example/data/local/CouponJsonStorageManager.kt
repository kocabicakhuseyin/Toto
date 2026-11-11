package com.example.data.local

import android.content.Context
import com.example.model.Coupon
import com.example.model.FormulaType
import com.example.model.PredictionOutcome
import com.example.model.TotoColumn
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SavedJsonCouponMeta(
    val id: String,
    val title: String,
    val formulaType: FormulaType,
    val totalColumns: Int,
    val createdAt: Long,
    val formattedDate: String,
    val file: File
)

object CouponJsonStorageManager {

    private const val DIRECTORY_NAME = "saved_coupons"

    private fun getStorageDir(context: Context): File {
        val dir = File(context.filesDir, DIRECTORY_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Serializes a Coupon into a clean, human-readable JSON string.
     */
    fun exportCouponToJsonString(coupon: Coupon): String {
        val root = JSONObject()
        root.put("id", coupon.id)
        root.put("title", coupon.title)
        root.put("formulaType", coupon.formulaType.name)
        root.put("formulaBadge", coupon.formulaType.badge)
        root.put("createdAt", coupon.createdAt)
        root.put("totalColumns", coupon.totalColumns)
        root.put("pricePerColumnTL", coupon.pricePerColumnTL)
        root.put("columnFeeTotalTL", coupon.columnFeeTotalTL)

        val columnsArray = JSONArray()
        coupon.columns.forEach { col ->
            val colObj = JSONObject()
            colObj.put("index", col.index)
            colObj.put("asString", col.asString)
            val predsArray = JSONArray()
            col.predictions.forEach { p ->
                predsArray.put(p.code)
            }
            colObj.put("predictions", predsArray)
            columnsArray.put(colObj)
        }
        root.put("columns", columnsArray)

        return root.toString(2)
    }

    /**
     * Parses a JSON string back into a Coupon domain object.
     */
    fun importCouponFromJsonString(jsonString: String): Result<Coupon> {
        return runCatching {
            val root = JSONObject(jsonString)
            val id = root.optString("id", "json_${System.currentTimeMillis()}")
            val title = root.optString("title", "JSON Yüklenen Kupon")
            val formulaName = root.optString("formulaType", FormulaType.GUARANTEE_14.name)
            val formulaType = try {
                FormulaType.valueOf(formulaName)
            } catch (e: Exception) {
                FormulaType.GUARANTEE_14
            }
            val createdAt = root.optLong("createdAt", System.currentTimeMillis())
            val pricePerCol = root.optDouble("pricePerColumnTL", 10.0)

            val columnsArray = root.getJSONArray("columns")
            val columns = mutableListOf<TotoColumn>()

            for (i in 0 until columnsArray.length()) {
                val colObj = columnsArray.getJSONObject(i)
                val idx = colObj.optInt("index", i + 1)
                val predsList = mutableListOf<PredictionOutcome>()

                if (colObj.has("predictions")) {
                    val pArray = colObj.getJSONArray("predictions")
                    for (pIdx in 0 until pArray.length()) {
                        val code = pArray.getString(pIdx)
                        predsList.add(
                            when (code) {
                                "1" -> PredictionOutcome.HOME
                                "2" -> PredictionOutcome.AWAY
                                else -> PredictionOutcome.DRAW
                            }
                        )
                    }
                } else if (colObj.has("asString")) {
                    val str = colObj.getString("asString")
                    str.forEach { ch ->
                        predsList.add(
                            when (ch) {
                                '1' -> PredictionOutcome.HOME
                                '2' -> PredictionOutcome.AWAY
                                else -> PredictionOutcome.DRAW
                            }
                        )
                    }
                }

                if (predsList.size == 15) {
                    columns.add(TotoColumn(index = idx, predictions = predsList))
                }
            }

            if (columns.isEmpty()) {
                throw IllegalArgumentException("JSON içinde 15 maçlık geçerli kolon bulunamadı!")
            }

            Coupon(
                id = id,
                title = title,
                formulaType = formulaType,
                columns = columns,
                createdAt = createdAt,
                pricePerColumnTL = pricePerCol
            )
        }
    }

    /**
     * Saves a Coupon to device internal storage in JSON format.
     */
    /**
     * Saves a Coupon to device internal storage in JSON format.
     */
    fun saveCouponToJsonFile(context: Context, coupon: Coupon): Result<File> {
        return runCatching {
            val dir = getStorageDir(context)
            val safeId = coupon.id.replace(Regex("[^a-zA-Z0-9_]"), "_")
            val file = File(dir, "coupon_$safeId.json")
            val jsonContent = exportCouponToJsonString(coupon)
            file.writeText(jsonContent, Charsets.UTF_8)
            file
        }
    }

    fun saveCouponToFile(context: Context, coupon: Coupon): Result<File> = saveCouponToJsonFile(context, coupon)

    /**
     * Lists all saved JSON coupons from device internal storage.
     */
    fun listSavedJsonCoupons(context: Context): List<SavedJsonCouponMeta> {
        val dir = getStorageDir(context)
        val files = dir.listFiles { f -> f.extension.equals("json", ignoreCase = true) } ?: return emptyList()

        val results = mutableListOf<SavedJsonCouponMeta>()
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

        files.forEach { file ->
            try {
                val jsonString = file.readText(Charsets.UTF_8)
                val root = JSONObject(jsonString)
                val id = root.optString("id", file.nameWithoutExtension)
                val title = root.optString("title", "Kayıtlı Kupon")
                val formulaName = root.optString("formulaType", FormulaType.GUARANTEE_14.name)
                val formulaType = try {
                    FormulaType.valueOf(formulaName)
                } catch (e: Exception) {
                    FormulaType.GUARANTEE_14
                }
                val colsCount = root.optInt("totalColumns", root.optJSONArray("columns")?.length() ?: 0)
                val createdAt = root.optLong("createdAt", file.lastModified())

                results.add(
                    SavedJsonCouponMeta(
                        id = id,
                        title = title,
                        formulaType = formulaType,
                        totalColumns = colsCount,
                        createdAt = createdAt,
                        formattedDate = dateFormat.format(Date(createdAt)),
                        file = file
                    )
                )
            } catch (e: Exception) {
                // Ignore corrupted file
            }
        }

        return results.sortedByDescending { it.createdAt }
    }

    /**
     * Loads a coupon from a specific JSON file on the device.
     */
    fun loadCouponFromFile(file: File): Result<Coupon> {
        return runCatching {
            val content = file.readText(Charsets.UTF_8)
            importCouponFromJsonString(content).getOrThrow()
        }
    }

    /**
     * Deletes a saved JSON coupon file from device storage.
     */
    fun deleteCouponFile(file: File): Boolean {
        return if (file.exists()) file.delete() else false
    }

    fun deleteSavedJsonCoupon(context: Context, couponId: String): Boolean {
        val dir = getStorageDir(context)
        val safeId = couponId.replace(Regex("[^a-zA-Z0-9_]"), "_")
        val file = File(dir, "coupon_$safeId.json")
        return deleteCouponFile(file)
    }
}
