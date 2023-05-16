package com.example.demobot.model;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface AppointmentRepository extends CrudRepository<Appointment,Long> {

    Set<Appointment> getAppointmentByUserId(Long chatId);
}
