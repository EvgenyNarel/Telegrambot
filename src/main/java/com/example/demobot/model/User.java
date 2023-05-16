package com.example.demobot.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    private Long id;

    @OneToMany(fetch = FetchType.EAGER)
//    @JoinColumn(name = "appointment_id")
    private Set<Appointment> appointmentSet;


    private String userName;

    private String firstName;

    private String lastName;

    private Timestamp registered;

    private boolean presenceRecord = false;

    private String address;

    private String numberPhone;

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", appointmentSet=" + appointmentSet +
                ", userName='" + userName + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", registered=" + registered + '\'' +
                ", presenceRecord=" + presenceRecord + '\'' +
                ", address=" + address + '\'' +
                ", numberPhone=" + numberPhone +
                '}';
    }

}
