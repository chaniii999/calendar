package com.calendar.app.service;

import com.calendar.app.entity.Schedule;
import com.calendar.app.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;

class SsePushServiceTest {

    @Test
    @DisplayName("subscribe는 emitter 반환")
    void subscribe_returnsEmitter() {
        SsePushService svc = new SsePushService();
        SseEmitter em = svc.subscribe("u1");
        assertThat(em).isNotNull();
    }

    @Test
    @DisplayName("구독 없으면 pushScheduleReminder는 false")
    void push_withoutSubscriber_false() {
        SsePushService svc = new SsePushService();
        User u = User.builder().id("u1").email("e@e").nickname("n").build();
        Schedule sc = Schedule.builder().id("s1").user(u).title("t").build();
        boolean delivered = svc.pushScheduleReminder(sc);
        assertThat(delivered).isFalse();
    }
}


