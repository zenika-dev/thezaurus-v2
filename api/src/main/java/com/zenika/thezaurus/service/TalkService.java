package com.zenika.thezaurus.service;

import com.zenika.thezaurus.model.Talk;
import com.zenika.thezaurus.repository.TalkRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.concurrent.ExecutionException;

@ApplicationScoped
public class TalkService {

    @Inject
    TalkRepository repository;

    public List<Talk> findAll() throws ExecutionException, InterruptedException {
        return repository.findAll();
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
        return repository.update(id, talk);
    }

    public boolean delete(String id) throws ExecutionException, InterruptedException {
        Talk existing = repository.findById(id);
        if (existing == null) {
            return false;
        }
        repository.delete(id);
        return true;
    }
}
