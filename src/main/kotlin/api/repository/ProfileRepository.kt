package api.repository

import com.lexa.api.config.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.*
import api.models.dto.GetProfileResponse
import api.models.dto.UpdateProfileRequest
import api.models.tables.UsersTable
import api.utils.toLocalDate
import org.jetbrains.exposed.sql.javatime.Date
import java.time.LocalDate
import java.time.LocalDateTime

class ProfileRepository {

    suspend fun getProfile(userId: Int): GetProfileResponse= dbQuery {
        UsersTable
            .slice(
                UsersTable.id,
                UsersTable.name,
                UsersTable.dateOfBirth,
                UsersTable.address,
                UsersTable.avatarUrl,
                UsersTable.email,
            )
            .select { UsersTable.id eq userId }
            .map {row ->

                val exposedLocalDate = row[UsersTable.dateOfBirth]
                val utilDate = exposedLocalDate?.let { java.sql.Date.valueOf(it) }

                GetProfileResponse(
                    id = row[UsersTable.id].value,
                    fullName = row[UsersTable.name],
                    DoB = utilDate ?: java.sql.Date.valueOf(row[UsersTable.dateOfBirth]),
                    address = row[UsersTable.address],
                    avatarUrl = row[UsersTable.avatarUrl],
                    email = row[UsersTable.email],
                )
            }
            .single()
    }

    suspend fun updateProfile(userId: Int, data: UpdateProfileRequest): Boolean = dbQuery {
        val updatedRows = UsersTable.update({ UsersTable.id eq userId }) {
            it[name] = data.fullName
            it[dateOfBirth] = data.DoB.toLocalDate()
            it[address] = data.address
        }

        updatedRows > 0
    }
}
