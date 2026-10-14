package com.mes.mesaichatbot.domain;

import com.mes.mesaichatbot.domain.user.dto.UserInfoResponseDTO;
import com.mes.mesaichatbot.domain.user.entity.UserRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "user_member")
@Getter
@Setter
public class UserMember implements UserDetails {
    @Id
    @Column(name = "employee_id")
    private Long employeeId;
    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    private Company companyId;
    @Column(name = "user_name", nullable = false, length = 50)
    private String username;
    @Column(name = "password", nullable = false)
    private String password;
    @Column(name = "company_email", nullable = false, unique = true)
    private String companyEmail;
    @Enumerated(EnumType.STRING)
    @Column(name = "role_code", nullable = false, length = 50)
    private UserRole roleCode;
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Override
    public @NonNull Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(this.roleCode.toString()));
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    /**
     * 사용자 엔티티를 DTO로 변환합니다.
     *
     * @return {@link UserInfoResponseDTO}.
     */
    public UserInfoResponseDTO toDto() {
        return new UserInfoResponseDTO(
                this.companyId.getCompanyId(), this.employeeId,
                this.username, this.companyEmail, this.roleCode);
    }
}
