package com.calendar.app.event;

import com.calendar.app.entity.Schedule;

public class ScheduleUpdatedEvent extends ScheduleEvent {
    
    public ScheduleUpdatedEvent(Object source, Schedule schedule) {
        super(source, schedule);
    }
}
