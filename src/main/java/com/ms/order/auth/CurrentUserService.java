package com.ms.order.auth;

import com.ms.order.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Serviço para obter informações do usuário atual.
 * As informações são extraídas dos headers HTTP que são injetados pelo API Gateway
 * após validação do JWT no serviço de autenticação.
 * 
 * Headers esperados:
 * - X-User-Id: ID do usuário
 * - X-User-Email: Email do usuário
 * - X-User-Role: Role do usuário (USER, ADMIN)
 * - X-Is-Admin: Se o usuário é admin (true/false)
 */
@Service
public class CurrentUserService {

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = 
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new ResourceNotFoundException("Authentication", "field", "No request context available");
        }
        return attributes.getRequest();
    }


    public Long getCurrentUserId() {
        String userId = getRequest().getHeader("X-User-Id");
        if (userId == null || userId.isBlank()) {
            throw new ResourceNotFoundException("Authentication", "field", "User ID not found in request");
        }
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException e) {
            throw new ResourceNotFoundException("Authentication", "field", "Invalid user ID format");
        }
    }

    public boolean isAdmin() {
        String isAdmin = getRequest().getHeader("X-Is-Admin");
        return "true".equalsIgnoreCase(isAdmin);
    }
}
