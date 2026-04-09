package api.services

import api.config.CloudinaryConfig
import com.cloudinary.utils.ObjectUtils
import java.io.File
import java.net.URLDecoder
import java.time.Instant
import java.util.UUID

object CloudinaryService {
    private val cloudinary = CloudinaryConfig.cloudinary

    fun uploadImage(fileBytes: ByteArray, directory: String): String? {
        val options = ObjectUtils.asMap(
            "folder", directory,
            "use_filename", false,
            "unique_filename", true,   // Cloudinary tự sinh chuỗi ký tự unique
            "overwrite", true,
            "resource_type", "auto"
        )

        return try {
            val uploadResult = cloudinary.uploader().upload(fileBytes, options)
            uploadResult["secure_url"] as String
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun uploadManyImages(filesBytes: List<ByteArray>, directory: String): List<String> {
        return filesBytes.mapNotNull { bytes ->
            uploadImage(bytes, directory)
        }
    }

    fun deleteImage(imageUrl: String): Map<*, *>? {
        val publicId = extractPublicId(imageUrl) ?: return null
        return try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("invalidate", true))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deleteManyImages(imageUrls: List<String>): Map<*, *>? {
        val publicIds = imageUrls.mapNotNull { extractPublicId(it) }
        if (publicIds.isEmpty()) return null

        return try {
            cloudinary.api().deleteResources(publicIds, ObjectUtils.emptyMap())
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun extractPublicId(url: String): String? {
        return try {
            // URL Cloudinary thường có dạng: .../upload/v1234567/folder/subfolder/public_id.jpg
            val splitUrl = url.split("/upload/")
            if (splitUrl.size < 2) return null

            val pathAfterUpload = splitUrl[1] // v1234567/folder/subfolder/public_id.jpg

            // Bỏ phần version (phần bắt đầu bằng 'v' và theo sau là số)
            val pathParts = pathAfterUpload.split("/").toMutableList()
            if (pathParts[0].startsWith("v") && pathParts[0].substring(1).all { it.isDigit() }) {
                pathParts.removeAt(0)
            }

            // Nối lại và bỏ phần mở rộng (extension) như .jpg, .png
            val fullPath = pathParts.joinToString("/")
            val publicId = fullPath.substringBeforeLast(".")

            // Decode URL (đề phòng trường hợp folder có dấu cách hoặc ký tự đặc biệt)
            URLDecoder.decode(publicId, "UTF-8")
        } catch (e: Exception) {
            null
        }
    }
}