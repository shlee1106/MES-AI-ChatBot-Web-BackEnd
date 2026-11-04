package com.mes.mesaichatbot.repository;

import com.mes.mesaichatbot.domain.PasswordResetToken;
import org.springframework.data.repository.CrudRepository;

public interface PasswordResetTokenRepository extends CrudRepository<PasswordResetToken, String> {
}
