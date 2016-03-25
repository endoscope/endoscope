package org.endoscope.cdi;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import java.lang.annotation.Annotation;

import org.endoscope.Endoscope;
import org.endoscope.properties.Properties;

public class CdiExtension implements Extension {
    private TypeChecker typeChecker = new TypeChecker(
            Properties.getScannedPackages(),
            Properties.getPackageExcludes(),
            Properties.getSupportedNames()
    );

    public <T> void processAnnotatedType(@Observes ProcessAnnotatedType<T> processAnnotatedType) {
        if (!Endoscope.isEnabled()) {
            return;
        }
        AnnotatedType<T> annotatedType = processAnnotatedType.getAnnotatedType();

        if (typeChecker.isNotSupported(annotatedType)) return;

        AnnotatedTypeWrapper<T> wrapper = new AnnotatedTypeWrapper<>(annotatedType, annotatedType.getAnnotations());
        wrapper.addAnnotation(new Annotation() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return WithEndoscope.class;
            }
        });
        processAnnotatedType.setAnnotatedType(wrapper);
    }
}