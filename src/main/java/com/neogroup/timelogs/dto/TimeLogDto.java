package com.neogroup.timelogs.dto;

import java.time.LocalDateTime;

public record TimeLogDto(Long id, LocalDateTime time) {
}
