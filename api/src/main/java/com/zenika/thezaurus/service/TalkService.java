package com.zenika.thezaurus.service;

import com.zenika.thezaurus.model.PagedResult;
import com.zenika.thezaurus.model.Role;
import com.zenika.thezaurus.model.Talk;
import com.zenika.thezaurus.model.TalkStatus;
import com.zenika.thezaurus.model.TemplateContextPage;
import com.zenika.thezaurus.repository.TalkRepository;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.concurrent.ExecutionException;

@ApplicationScoped
public class TalkService {

    @Inject
    TalkRepository repository;

    @Inject
    SecurityIdentity identity;

    public List<Talk> findAll() throws ExecutionException, InterruptedException {
        return repository.findAll();
    }

    public TemplateContextPage findContextOptions(String cursor) throws ExecutionException, InterruptedException {
        return repository.findContextOptions(cursor);
    }

    public PagedResult<Talk> findPaged(int page, int pageSize, TalkStatus status, boolean personal)
            throws ExecutionException, InterruptedException {
        String currentEmail = personal ? getCurrentUserEmail() : null;
        return repository.findPaged(page, pageSize, status, currentEmail);
    }

    public Talk findById(String id) throws ExecutionException, InterruptedException {
        return repository.findById(id);
    }

    public Talk create(Talk talk) throws ExecutionException, InterruptedException {
        return repository.create(talk);
    }

    public Talk update(String id, Talk talk) throws ExecutionException, InterruptedException {
        Talk existing = repository.findById(id);
        if (existing == null) {
            return null;
        }
        String email = getCurrentUserEmail();
        boolean privileged = isPrivilegedUser();
        return repository.update(id, talk, stored -> stored.canBeEditedBy(email, privileged));
    }

    public boolean delete(String id) throws ExecutionException, InterruptedException {
        Talk existing = repository.findById(id);
        if (existing == null) {
            return false;
        }
        repository.delete(id);
        return true;
    }

    private String getCurrentUserEmail() {
        if (identity == null || identity.getPrincipal() == null) {
            return null;
        }
        String name = identity.getPrincipal().getName();
        return (name == null || name.isBlank()) ? null : name.trim();
    }

    private boolean isPrivilegedUser() {
        return identity != null && (identity.hasRole(Role.Names.ADMIN) || identity.hasRole(Role.Names.DT));
    }
}
