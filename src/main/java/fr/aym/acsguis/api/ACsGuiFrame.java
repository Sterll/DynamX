// TODO port:1.20.1 stub - ACsGuiFrame annotation
package fr.aym.acsguis.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ACsGuiFrame {
    String value() default "";

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface RegisteredStyleSheet {
    }
}
