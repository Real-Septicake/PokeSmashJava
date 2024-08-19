package models;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.constant.Constable;

@Retention(RetentionPolicy.RUNTIME)
public @interface Col {
    String name();
    Class<? extends Constable> type();
}
