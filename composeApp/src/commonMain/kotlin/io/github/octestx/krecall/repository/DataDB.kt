package io.github.octestx.krecall.repository

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.klogging.noCoLogger
import kotlinx.io.files.Path
import models.sqld.DataDBQueries
import models.sqld.DataItem

object DataDB {
    private val ologger = noCoLogger<DataDB>()
    private lateinit var driver: SqlDriver
    private lateinit var dataDBQueries: DataDBQueries

    fun init(dbFile: Path) {
        driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.toString().apply { ologger.info("loadSql: $this") }}")
        driver.execute(null, "CREATE TABLE IF NOT EXISTS DataItem (timestamp INTEGER NOT NULL PRIMARY KEY, data TEXT DEFAULT NULL, processed INTEGER NOT NULL DEFAULT 0, mark TEXT DEFAULT NULL);", 0)
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

    fun searchDataInAll(query: String): List<DataItem> {
        return dataDBQueries.searchDataInAll("%$query%").executeAsList()
    }

    fun searchDataWithTimeRange(startTimestamp: Long, endTimestamp: Long, query: String): List<DataItem> {
        return dataDBQueries.searchDataWithTimeRange(startTimestamp, endTimestamp, "%$query%").executeAsList()
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

    fun markScreenData(timestamp: Long, mark: String) {
        dataDBQueries.markScreenData(mark, timestamp)
    }

    fun listTimestampWithMark(mark: String): List<Long> {
        return dataDBQueries.listTimestampWithMark(mark).executeAsList()
    }

    fun listTimestampWithNotMark(mark: String): List<Long> {
        return dataDBQueries.listTimestampWithNotMark(mark).executeAsList()
    }
}
