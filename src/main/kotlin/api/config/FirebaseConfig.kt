package api.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import io.ktor.server.application.*
import java.io.InputStream

class FirebaseConfig {

    companion object {
         fun initFirebase() {
            // Đọc file json từ thư mục resources
            val serviceAccount: InputStream? =
                object {}.javaClass.classLoader.getResourceAsStream("firebase-service.json")

            if (serviceAccount != null) {
                val options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build()

                if (FirebaseApp.getApps().isEmpty()) {
                    FirebaseApp.initializeApp(options)
                    println("✅ Firebase Admin SDK initialized successfully!")
                }
            } else {
                println("❌ Không tìm thấy file firebase-service.json")
            }
        }
    }

}