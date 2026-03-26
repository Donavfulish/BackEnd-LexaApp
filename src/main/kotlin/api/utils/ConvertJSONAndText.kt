package api.utils

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.StringColumnType
import org.jetbrains.exposed.sql.Table
import org.postgresql.util.PGobject

fun Table.jsonb(name: String): Column<String> = registerColumn(name, object : StringColumnType() {
    override fun sqlType(): String = "jsonb"

    override fun valueFromDB(value: Any): String = if (value is PGobject) value.value ?: "" else value.toString()

    override fun notNullValueToDB(value: Any): Any = PGobject().apply {
        type = "jsonb"
        this.value = value as String
    }
})