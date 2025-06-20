package com.example.globalmethodauthorization.evaluator;

import com.example.globalmethodauthorization.domain.Document;
import com.example.globalmethodauthorization.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
@RequiredArgsConstructor
public class DocumentPermissionEvaluator implements PermissionEvaluator {

    private final DocumentRepository documentRepository;

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        Document document = (Document) targetDomainObject;
        String p = (String) permission;

        boolean admin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(p));

        return admin || document.getOwner().equals(authentication.getName());
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        String code = targetId.toString();
        Document document = documentRepository.findDocument(code);

        String p = (String) permission;

        boolean admin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(p));

        return admin || document.getOwner().equals(authentication.getName());
    }
}
