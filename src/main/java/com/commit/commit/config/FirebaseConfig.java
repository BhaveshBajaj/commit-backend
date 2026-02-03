package com.commit.commit.config;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.credentials.path:}")
    private String firebaseCredentialsPath;

    @Value("${firebase.credentials.json:}")
    private String firebaseCredentialsJson;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            GoogleCredentials credentials;

            // Check if JSON content is provided via environment variable (for Railway/production)
            if (firebaseCredentialsJson != null && !firebaseCredentialsJson.isEmpty()) {
                ByteArrayInputStream serviceAccount = new ByteArrayInputStream(
                    firebaseCredentialsJson.getBytes(StandardCharsets.UTF_8)
                );
                credentials = GoogleCredentials.fromStream(serviceAccount);
            } else if (firebaseCredentialsPath != null && !firebaseCredentialsPath.isEmpty()) {
                // Use file path (for local development)
                FileInputStream serviceAccount = new FileInputStream(firebaseCredentialsPath);
                credentials = GoogleCredentials.fromStream(serviceAccount);
            } else {
                throw new IllegalStateException(
                    "Firebase credentials not configured. Set either FIREBASE_CREDENTIALS_JSON or FIREBASE_CREDENTIALS_PATH environment variable"
                );
            }
            
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build();

            return FirebaseApp.initializeApp(options);
        }
        return FirebaseApp.getInstance();
    }

    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        return FirebaseAuth.getInstance(firebaseApp);
    }
}
