package com.example.demobot.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
public class User {

    @Id
    private Long id;

    private String userName;

    private String firstName;

    private String lastName;

    private Timestamp registered;

    private boolean presenceRecord = false;

    private Timestamp dataAppointment;

    private Timestamp timeAppointment;

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", userName='" + userName + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", registered=" + registered +
                '}';
    }
}
