package com.calendar.app.dto.schedule;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReminderEnabledResponse {
    private String scheduleId;
    private boolean enabled;
}



