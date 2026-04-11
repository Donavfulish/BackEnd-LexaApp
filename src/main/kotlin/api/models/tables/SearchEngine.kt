package api.models.tables

import api.models.dto.SearchResponse
import com.lexa.api.config.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.IntegerColumnType
import org.jetbrains.exposed.sql.TextColumnType
import org.jetbrains.exposed.sql.statements.StatementType
import org.jetbrains.exposed.sql.transactions.TransactionManager

object SearchEngine {
    suspend fun searchAllCourses(query: String) : List<SearchResponse> = dbQuery {
        val results = mutableListOf<SearchResponse>()
        val sql =
            """
              WITH search AS (
              SELECT 
                id,
                ts_rank(search_vector, websearch_to_tsquery('simple', f_unaccent(?))) AS fts_rank,
                similarity(f_unaccent(title), f_unaccent(?)) AS trigram_rank
              FROM courses
              WHERE 
                privacy = 'PUBLIC' AND (
                search_vector @@ websearch_to_tsquery('simple', f_unaccent(?)) 
                OR f_unaccent(title) % f_unaccent(?)
                OR f_unaccent(title) ILIKE f_unaccent(?)                    ))
  
            SELECT id, (fts_rank * 0.7 + trigram_rank * 0.3) AS final_score
            FROM search
            ORDER BY final_score DESC
            """.trimIndent()
        TransactionManager.current().exec(
            sql,
            args = listOf(
                TextColumnType() to query,
                TextColumnType() to query,
                TextColumnType() to query,
                TextColumnType() to query,
                TextColumnType() to "%$query%"
            ),
            explicitStatementType = StatementType.SELECT
        ) { resultSet ->
            while (resultSet.next()){
                results.add(
                    SearchResponse(
                        id = resultSet.getLong("id"),
                        score = resultSet.getFloat("final_score")
                    )
                )
            }
        }
        results
    }

    suspend fun searchMyCourses(query: String, userId: Int) : List<SearchResponse> = dbQuery {
        val results = mutableListOf<SearchResponse>()
        val sql =
            """
                WITH search AS (
              SELECT 
                id,
                ts_rank(search_vector, websearch_to_tsquery('simple', f_unaccent(?))) AS fts_rank,
                similarity(f_unaccent(title), f_unaccent(?)) AS trigram_rank
              FROM courses
              WHERE 
                creator_id = ?
                AND (
                search_vector @@ websearch_to_tsquery('simple', f_unaccent(?)) 
                OR f_unaccent(title) % f_unaccent(?)
                OR f_unaccent(title) ILIKE f_unaccent(?)                    ))
  
            SELECT id, (fts_rank * 0.7 + trigram_rank * 0.3) AS final_score
            FROM search
            ORDER BY final_score DESC
            """.trimIndent()
        TransactionManager.current().exec(
            sql,
            args = listOf(
                TextColumnType() to query,
                TextColumnType() to query,
                IntegerColumnType() to userId,
                TextColumnType() to query,
                TextColumnType() to query,
                TextColumnType() to "%$query%"
            ),
            explicitStatementType = StatementType.SELECT
        ) { resultSet ->
            while (resultSet.next()){
                results.add(
                    SearchResponse(
                        id = resultSet.getLong("id"),
                        score = resultSet.getFloat("final_score")
                    )
                )
            }
        }
        results
    }

    suspend fun  searchFavoriteCourses(query: String, userId: Int) : List<SearchResponse> = dbQuery {
        val results = mutableListOf<SearchResponse>()
        val sql =
            """
             WITH search AS (
                SELECT 
                  id,
                  ts_rank(search_vector, websearch_to_tsquery('simple', f_unaccent(?))) AS fts_rank,
                  similarity(f_unaccent(title), f_unaccent(?)) AS trigram_rank
                FROM courses JOIN user_favorite_courses ON courses.id = user_favorite_courses.course_id
                WHERE 
                  user_favorite_courses.user_id = ?
                 AND (
                  search_vector @@ websearch_to_tsquery('simple', f_unaccent(?)) 
                  OR f_unaccent(title) % f_unaccent(?)
                  OR f_unaccent(title) ILIKE f_unaccent(?)))
              
            SELECT id, (fts_rank * 0.7 + trigram_rank * 0.3) AS final_score
            FROM search
            ORDER BY final_score DESC
            """.trimIndent()
        TransactionManager.current().exec(
            sql,
            args = listOf(
                TextColumnType() to query,
                TextColumnType() to query,
                IntegerColumnType() to userId,
                TextColumnType() to query,
                TextColumnType() to query,
                TextColumnType() to "%$query%"
            ),
            explicitStatementType = StatementType.SELECT
        ) { resultSet ->
            while (resultSet.next()){
                results.add(
                    SearchResponse(
                        id = resultSet.getLong("id"),
                        score = resultSet.getFloat("final_score")
                    )
                )
            }
        }
        results
    }
}