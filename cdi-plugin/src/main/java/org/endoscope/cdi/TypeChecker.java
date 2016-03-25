package org.endoscope.cdi;

import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.ws.rs.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

public class TypeChecker {
    private static final Logger log = getLogger(TypeChecker.class);
    public static final List<Class> SUPPORTED_ANNOTATIONS;

    static {
        List<Class> classes = new ArrayList<>();
        classes.add(ApplicationScoped.class);
        classes.add(Path.class);
        try{
            log.info("Loading EJB classes - if present");
            Class classSingleton = Class.forName("javax.ejb.Singleton");
            Class classStatefull = Class.forName("javax.ejb.Stateful");
            Class classStateless = Class.forName("javax.ejb.Stateless");
            classes.add(classSingleton);
            classes.add(classStatefull);
            classes.add(classStateless);
        }catch(Exception e){
            log.info("Didn't load EJB classes");
        }
        SUPPORTED_ANNOTATIONS = Collections.unmodifiableList(classes);
    }

    private List<String> scannedPackages;
    private List<String> excludedPackages;
    private Pattern supportedNamesPattern;

    public TypeChecker(String[] scannedPackages, String[] excludedPackages, String supportedNames){
        this.scannedPackages = cleanupStrings(scannedPackages);
        this.excludedPackages = cleanupStrings(excludedPackages);
        this.supportedNamesPattern = Pattern.compile(supportedNames);
    }

    private List<String> cleanupStrings(String[] in){
        if( in == null || in.length == 0 ){
            return Collections.EMPTY_LIST;
        }
        return Arrays.stream(in)
                .filter( s -> s != null)
                .map( s -> s.trim() )
                .filter( s -> s.length() > 0)
                .collect(Collectors.toList());
    }

    private boolean isValidType(AnnotatedType type) {
        if( type.getJavaClass() != null && type.getJavaClass().getCanonicalName() != null ){
            return true;
        }
        return false;
    }

    private boolean isAlreadyAnnotated(AnnotatedType type) {
        if( type.isAnnotationPresent(WithEndoscope.class) ){
            return true;
        }
        return false;
    }

    private boolean isSupportedType(AnnotatedType type) {
        for( Class c : SUPPORTED_ANNOTATIONS ){
            if( type.isAnnotationPresent(c) ){
                return true;
            }
        }
        return false;
    }

    private boolean isSupportedName(AnnotatedType type) {
        String name = type.getJavaClass().getSimpleName();
        if( supportedNamesPattern.matcher(name).matches() ){
            return true;
        }
        return false;
    }

    private boolean isInScannedPackage(String canonicalName){
        if( scannedPackages == null || scannedPackages.isEmpty() ){
            return true; //by default in scanned package
        }

        for( String pkg : scannedPackages){
            if( canonicalName.startsWith(pkg) ){
                return true;
            }
        }
        return false;
    }

    private boolean isInExcludedPackage(String canonicalName){
        if( excludedPackages == null || excludedPackages.isEmpty() ){
            return false; //by default not excluded
        }

        for( String pkg : excludedPackages){
            if( canonicalName.startsWith(pkg) ){
                log.info("isInExcludedPackage: {}", canonicalName);
                return true;
            }
        }
        return false;
    }

    public boolean isNotSupported(AnnotatedType annotatedType) {
        if( isAlreadyAnnotated(annotatedType) ){
            log.info("SUPPORTED: is already annotated: {}", annotatedType.getJavaClass());
            return true;
        }

        if( !isValidType(annotatedType) ){
            log.info("class is invalid: {}", annotatedType.getJavaClass());
            return true;
        }

        String canonicalName = annotatedType.getJavaClass().getCanonicalName();
        if( !isInScannedPackage(canonicalName) ){
            log.info("not in scanned package: {}", annotatedType.getJavaClass());
            return true;
        }
        if( isInExcludedPackage(canonicalName) ){
            log.info("is in excluded package: {}", annotatedType.getJavaClass());
            return true;
        }

        if( !isSupportedType(annotatedType) && !isSupportedName(annotatedType)){
            log.info("doesn't have supported annotation nor matching class name: {}", annotatedType.getJavaClass());
            return true;
        }

        log.info("SUPPORTED: {}", annotatedType.getJavaClass());
        return false;
    }
}
