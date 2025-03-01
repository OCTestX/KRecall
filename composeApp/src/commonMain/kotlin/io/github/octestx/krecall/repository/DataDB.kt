package io.github.octestx.krecall.repository

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.klogging.noCoLogger
import kotlinx.io.files.Path
import models.sqld.DataDBQueries

object DataDB {
    private val ologger = noCoLogger<DataDB>()
    private lateinit var driver: SqlDriver
    private lateinit var dataDBQueries: DataDBQueries
    fun init(dbFile: Path) {
        driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.toString().apply { ologger.info("loadSql: $this") }}")
        dataDBQueries = DataDBQueries(driver)
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