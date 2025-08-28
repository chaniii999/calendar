package com.calendar.app.event;

import com.calendar.app.entity.Schedule;

public class ScheduleCreatedEvent extends ScheduleEvent {
    
    public ScheduleCreatedEvent(Object source, Schedule schedule) {
        super(source, schedule);
    }
}
