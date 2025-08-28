package com.calendar.app.event;

import com.calendar.app.entity.Schedule;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public abstract class ScheduleEvent extends ApplicationEvent {
    
    private final Schedule schedule;
    
    public ScheduleEvent(Object source, Schedule schedule) {
        super(source);
        this.schedule = schedule;
    }
}
