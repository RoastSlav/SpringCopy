package SpringWeb.Annotation;

import java.lang.annotation.Retention;

@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface PostMapping {
    String value();
}
