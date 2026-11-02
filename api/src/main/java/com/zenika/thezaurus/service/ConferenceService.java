package com.zenika.thezaurus.service;

import com.zenika.thezaurus.model.Conference;
import com.zenika.thezaurus.repository.ConferenceRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.concurrent.ExecutionException;

@ApplicationScoped
public class ConferenceService {

    @Inject
    ConferenceRepository repository;

    public List<Conference> findAll() throws ExecutionException, InterruptedException {
        return repository.findAll();
    }

    public Conference findById(String id) throws ExecutionException, InterruptedException {
        return repository.findById(id);
    }

    public Conference create(Conference conference) throws ExecutionException, InterruptedException {
        return repository.create(conference);
    }

    public Conference update(String id, Conference conference) throws ExecutionException, InterruptedException {
        Conference existing = repository.findById(id);
        if (existing == null) {
            return null;
        }
        return repository.update(id, conference);
    }

    public boolean delete(String id) throws ExecutionException, InterruptedException {
        Conference existing = repository.findById(id);
        if (existing == null) {
            return false;
        }
        repository.delete(id);
        return true;
    }
}
