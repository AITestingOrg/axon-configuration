package org.aist.libs.eventsourcing.configuration.eventstore;

import java.lang.annotation.*;

@Retention(value = RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Inherited
public @interface Name {
    String value();
}
