package api.repository

import com.lexa.api.config.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.*
import api.models.dto.GetProfileResponse
import api.models.tables.UsersTable

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
                    DoB = utilDate,
                    address = row[UsersTable.address],
                    avatarUrl = row[UsersTable.avatarUrl],
                    email = row[UsersTable.email],
                )
            }
            .single()
    }
}
