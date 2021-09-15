package com.protei.task.systemuser;

import com.protei.task.systemuser.enumeration.SystemUserStatus;
import lombok.*;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class SystemUser {
    @Id
    @SequenceGenerator(
            name = "user_sequence",
            sequenceName = "user_sequence",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "user_sequence"
    )
    private long id;
    private String name;
    private String email;
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    private SystemUserStatus userStatus;

    public SystemUser(String name,
                      String email,
                      String phoneNumber,
                      SystemUserStatus userStatus) {
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.userStatus = userStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SystemUser user = (SystemUser) o;
        return id == user.id &&
                Objects.equals(name, user.name) &&
                Objects.equals(email, user.email) &&
                Objects.equals(phoneNumber, user.phoneNumber) &&
                userStatus == user.userStatus;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, email, phoneNumber, userStatus);
    }
}
