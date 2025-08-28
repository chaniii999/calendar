package com.calendar.app.event;

import com.calendar.app.entity.Schedule;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ReminderTimeEvent extends ScheduleEvent {
    
    private final LocalDateTime reminderTime;
    
    public ReminderTimeEvent(Object source, Schedule schedule, LocalDateTime reminderTime) {
        super(source, schedule);
        this.reminderTime = reminderTime;
    }
}
