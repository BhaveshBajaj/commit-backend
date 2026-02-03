package com.commit.commit.security;

import java.io.IOException;
import java.time.OffsetDateTime;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.commit.commit.entity.User;
import com.commit.commit.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(1)
public class FirebaseAuthFilter extends OncePerRequestFilter {

    private final FirebaseAuth firebaseAuth;
    private final UserRepository userRepository;

    public FirebaseAuthFilter(FirebaseAuth firebaseAuth, UserRepository userRepository) {
        this.firebaseAuth = firebaseAuth;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // Allow OPTIONS requests (CORS preflight) to pass through without authentication
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            // Add CORS headers explicitly for OPTIONS requests
            response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin"));
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH");
            response.setHeader("Access-Control-Allow-Headers", "*");
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Max-Age", "3600");
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }
        
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendUnauthorized(response, "No token provided");
            return;
        }

        String idToken = authHeader.substring(7);

        try {
            FirebaseToken decodedToken = firebaseAuth.verifyIdToken(idToken);
            String firebaseUid = decodedToken.getUid();
            String email = decodedToken.getEmail();
            String name = decodedToken.getName();

            // Find or create user (auto-registration)
            User user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseGet(() -> createUser(firebaseUid, email, name));

            // Set user in thread-local context
            AuthenticatedUser.set(user);

            filterChain.doFilter(request, response);

        } catch (FirebaseAuthException e) {
            if (e.getMessage().contains("expired")) {
                sendUnauthorized(response, "Token expired");
            } else {
                sendUnauthorized(response, "Invalid token");
            }
        } finally {
            AuthenticatedUser.clear();
        }
    }

    private User createUser(String firebaseUid, String email, String name) {
        User user = new User();
        user.setFirebaseUid(firebaseUid);
        user.setEmail(email);
        user.setName(name != null ? name : email.split("@")[0]);
        user.setCreatedAt(OffsetDateTime.now());
        return userRepository.save(user);
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }
}
