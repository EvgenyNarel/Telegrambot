package com.example.demobot.model;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MassageRepository extends CrudRepository<Massage, Long> {
    Massage getByName(String name);
}
