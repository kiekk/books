package com.example.globalmethodauthorization.service;

import com.example.globalmethodauthorization.domain.Document;
import com.example.globalmethodauthorization.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;

    @PostAuthorize("hasPermission(returnObject, 'ROLE_ADMIN')")
    public Document getDocument(String code) {
        return documentRepository.findDocument(code);
    }

    @PreAuthorize("hasPermission(#code, 'document', 'ROLE_ADMIN')")
    public Document getDocumentPre(String code) {
        return documentRepository.findDocument(code);
    }

}
