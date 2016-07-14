package com.github.endoscope.cdi;

import com.github.endoscope.Endoscope;
import com.github.endoscope.properties.Properties;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.*;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

public class CdiExtension implements Extension {
    private TypeChecker typeChecker = new TypeChecker(
            Properties.getScannedPackages(),
            Properties.getPackageExcludes(),
            Properties.getSupportedNames()
    );

    //CDI 1.0 eager startup workaround
    private List<Bean> startupBeans = new ArrayList<>();

    public <T> void processAnnotatedType(@Observes ProcessAnnotatedType<T> processAnnotatedType) {
        if (!Endoscope.isEnabled()) {
            return;
        }
        AnnotatedType<T> annotatedType = processAnnotatedType.getAnnotatedType();

        if( AppLifecycleManager.class.equals(annotatedType.getJavaClass()) ){
            return;
        }

        if (typeChecker.isNotSupported(annotatedType)){
            return;
        }

        AnnotatedTypeWrapper<T> wrapper = new AnnotatedTypeWrapper<>(annotatedType, annotatedType.getAnnotations());
        wrapper.addAnnotation(new Annotation() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return WithEndoscope.class;
            }
        });
        processAnnotatedType.setAnnotatedType(wrapper);
    }

    <X> void processBean(@Observes ProcessBean<X> event) {
        if (!Endoscope.isEnabled()) {
            return;
        }
        if( event.getAnnotated().isAnnotationPresent(EndoscopeStartup.class) ){
            startupBeans.add(event.getBean());
        }
    }

    void afterDeploymentValidation(@Observes AfterDeploymentValidation event, BeanManager manager) {
        if (!Endoscope.isEnabled()) {
            return;
        }
        for( Bean bean : startupBeans) {
            manager.getReference(bean, bean.getBeanClass(), manager.createCreationalContext(bean)).toString();
        }
    }
}