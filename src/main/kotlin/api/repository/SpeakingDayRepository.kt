package api.repository

import api.models.dto.ShortParagraphDto
import api.models.dto.ShortParagraphSpeakingDayDto
import api.models.tables.SpeakingDaysTable
import api.models.tables.SpeakingParagraphResultsTable
import api.models.tables.SpeakingParagraphsTable
import com.lexa.api.config.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.select

class SpeakingDayRepository {
    suspend fun getParagraphSpeakingDay(speakingDayId: Long): List<ShortParagraphSpeakingDayDto> = dbQuery {

        SpeakingDaysTable
            .select(SpeakingDaysTable.id eq speakingDayId)
            .map { row ->

                val list_paragraphs = SpeakingParagraphsTable
                    .select { SpeakingParagraphsTable.speakingDayId eq speakingDayId }
                    .orderBy(SpeakingParagraphsTable.paragraphOrder to SortOrder.ASC)
                    .map { row ->


                        ShortParagraphDto(
                            id = row[SpeakingParagraphsTable.id].value,
                            paragraph = row[SpeakingParagraphsTable.paragraph],
                            paragraph_order = row[SpeakingParagraphsTable.paragraphOrder]
                        )
                    }


                ShortParagraphSpeakingDayDto(
                    title = row[SpeakingDaysTable.title],
                    list_paragraphs = list_paragraphs,
                )
            }

    }
}