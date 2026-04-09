package api.config

import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import io.github.cdimascio.dotenv.dotenv
import io.github.cdimascio.dotenv.dotenv

object CloudinaryConfig {
    private val config = mapOf(
        "cloud_name" to dotenv["CLOUDINARY_CLOUD_NAME"],
        "api_key" to dotenv["CLOUDINARY_API_KEY"],
        "api_secret" to dotenv["CLOUDINARY_SECRET_KEY"]
    )
    val cloudinary = Cloudinary(config)
}