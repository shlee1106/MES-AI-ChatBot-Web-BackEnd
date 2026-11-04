package com.mes.mesaichatbot.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@NoArgsConstructor
@AllArgsConstructor
// 복합키생성을 위해 만든 클래스가 userMember에 속해있다는 것을 알리는 어노테이션
@EqualsAndHashCode
// UserMember와 동일한 객체로 인식하기 위한 어노테이션
@Embeddable
public class UserMemberId implements Serializable {
    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;
}