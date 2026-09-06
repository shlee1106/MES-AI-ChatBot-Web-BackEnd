package com.mes.mesaichatbot.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_member")
@Getter
@Setter
public class UserMember {
    @Id
    @Column(name = "employee_id")
    private int employeeId;
    @Column(name = "company_id", nullable = false)
    private int companyId;
    @Column(name = "user_name", nullable = false, length = 50)
    private String username;
    @Column(name = "company_email", nullable = false, unique = true)
    private String companyEmail;
    @Column(name = "role_code", nullable = false, length = 50)
    private String roleCode;
    @Column(name = "user_status_code", nullable = false, length = 20)
    private String userStatusCode;
    @Column(name = "auth_code")
    private int authCode;
    @Column(name = "auth_expired_at")
    private LocalDateTime authExpiredAt;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
