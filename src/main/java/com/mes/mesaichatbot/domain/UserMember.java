package com.mes.mesaichatbot.domain;

import com.mes.mesaichatbot.domain.user.entity.UserRole;
import jakarta.persistence.*;
import lombok.*;
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
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserMember implements UserDetails {

    @EmbeddedId
    private UserMemberId id;

    @MapsId("companyId")
    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

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

    /**
     * 사원번호(Long)와 회사 엔티티로 바로 생성할 수 있는 호환 생성자.
     * 내부적으로 {@link UserMemberId} 복합키 객체를 생성하여 매핑합니다.
     */
    public UserMember(Long employeeId, Company company, String username, String password, String companyEmail, UserRole roleCode, LocalDateTime createdAt) {
        this.id = new UserMemberId(company != null ? company.getCompanyId() : null, employeeId);
        this.company = company;
        this.username = username;
        this.password = password;
        this.companyEmail = companyEmail;
        this.roleCode = roleCode;
        this.createdAt = createdAt;
    }

    // 객체(복합키)에 들어간 컬럼 오류방지용 설정자
    public Long getEmployeeId() {
        return this.id != null ? this.id.getEmployeeId() : null;
    }

    public Company getCompanyId() {
        return this.company;
    }

    @Override
    public @NonNull Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(this.roleCode.toString()));
    }
}
