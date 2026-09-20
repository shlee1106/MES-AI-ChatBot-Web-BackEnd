package com.mes.mesaichatbot.domain.email.repository;

import com.mes.mesaichatbot.domain.email.entity.PasswordResetToken;
import org.springframework.data.repository.CrudRepository;

public interface PasswordResetTokenRepository extends CrudRepository<PasswordResetToken, String> {
}
