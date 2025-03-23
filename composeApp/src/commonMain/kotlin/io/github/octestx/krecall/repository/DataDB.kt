package io.github.octestx.krecall.repository

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.github.octestx.krecall.plugins.basic.AIResult
import io.github.octestx.krecall.plugins.basic.exceptionSerializableOjson
import io.klogging.noCoLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path
import models.sqld.DataDBQueries
import models.sqld.DataItem

object DataDB {
    private val ologger = noCoLogger<DataDB>()
    private lateinit var driver: SqlDriver
    private lateinit var dataDBQueries: DataDBQueries

    fun init(dbFile: Path) {
        driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.toString().apply { ologger.info("loadSql: $this") }}")

        //TODO 手动建表
        driver.execute(
            null, """
                CREATE TABLE IF NOT EXISTS "DataItem" (
                    "screenId"	INTEGER NOT NULL DEFAULT 0,
                    "timestamp"	INTEGER NOT NULL,
                    "data"	TEXT DEFAULT NULL,
                    "status"	INTEGER NOT NULL DEFAULT 0,
                    "error"	TEXT DEFAULT NULL,
                    "ocr"	TEXT DEFAULT NULL,
                    "mark"	TEXT NOT NULL,
                    PRIMARY KEY("timestamp")
                );
            """.trimIndent(), 0
        )
        ologger.info("Table DataItem created or verified.")
        dataDBQueries = DataDBQueries(driver)
    }

    fun listAllData(): List<DataItem> {
        return dataDBQueries.listAllData().executeAsList()
    }

    fun listDataWithTimestampRange(startTimestamp: Long, endTimestamp: Long): List<DataItem> {
        return dataDBQueries.listDataWithTimestampRange(startTimestamp, endTimestamp).executeAsList()
    }

    fun getData(timestamp: Long): DataItem? {
        return dataDBQueries.getData(timestamp).executeAsOneOrNull()
    }

    fun listNotProcessedData(): List<DataItem> {
        return dataDBQueries.listNotProcessedData().executeAsList()
    }

    suspend fun searchDataInAll(queries: List<String>): List<DataItem> {
        return withContext(Dispatchers.IO) {
            if (queries.isEmpty()) return@withContext emptyList()

            // 按结果集大小排序，从小集合开始交
            val sortedSets = queries.map { query ->
                dataDBQueries.searchDataInAll("%$query%").executeAsList().toSet()
            }.sortedBy { it.size }

            return@withContext sortedSets.reduceOrNull { acc, set ->
                acc.intersect(set).takeIf { it.isNotEmpty() } ?: return@withContext emptyList()
            }?.toList() ?: emptyList()
        }
    }

    fun searchDataWithTimeRange(startTimestamp: Long, endTimestamp: Long, queries: List<String>): List<DataItem> {
        if (queries.isEmpty()) return emptyList()

        // 按结果集大小排序，从小集合开始交
        val sortedSets = queries.map { query ->
            dataDBQueries.searchDataWithTimeRange(startTimestamp, endTimestamp, "%$query%").executeAsList().toSet()
        }.sortedBy { it.size }

        return sortedSets.reduceOrNull { acc, set ->
            acc.intersect(set).takeIf { it.isNotEmpty() } ?: return emptyList()
        }?.toList() ?: emptyList()
    }

    fun addNewRecord(timestamp: Long) {
        dataDBQueries.addNewRecord(timestamp)
    }

    fun appendData(timestamp: Long, data: String) {
        dataDBQueries.appendData(data, timestamp)
    }

    fun processed(timestamp: Long) {
        dataDBQueries.processed(timestamp)
    }

    fun addMark(timestamp: Long, mark: String) {
        val data = dataDBQueries.getData(timestamp).executeAsOneOrNull() ?: return
        val newMark = data.mark + "\n" + mark
        dataDBQueries.markScreenData(newMark, timestamp)
    }

    fun removeMark(timestamp: Long, mark: String) {
        val data = dataDBQueries.getData(timestamp).executeAsOneOrNull() ?: return
        data.mark.split("\n").filter { it != mark }.joinToString("\n").let {
            dataDBQueries.markScreenData(it, timestamp)
        }
    }

    fun listTimestampWithMark(mark: String): List<Long> {
        return dataDBQueries.listTimestampWithMark(mark).executeAsList()
    }

    fun listTimestampWithNotMark(mark: String): List<Long> {
        return dataDBQueries.listTimestampWithNotMark(mark).executeAsList()
    }

    fun happenError(timestamp: Long, error: AIResult.Failed<String>) {
        dataDBQueries.happenError(exceptionSerializableOjson.encodeToString(error), timestamp)
    }

    fun appendOCRData(timestamp: Long, data: String) {
        dataDBQueries.appendOCRData(data, timestamp)
    }

    fun getLeastTimestamp(): Long? {
        return dataDBQueries.getLeastTimestamp().executeAsOneOrNull()
    }
}
