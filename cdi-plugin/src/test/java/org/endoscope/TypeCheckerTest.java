package org.endoscope;

import javax.enterprise.inject.spi.AnnotatedType;

import org.endoscope.cdi.TypeChecker;
import org.endoscope.cdi.WithEndoscope;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class TypeCheckerTest {
    @Mock
    private AnnotatedType type;

    private void assertNotSupported(TypeChecker tc) {
        assertTrue(tc.isNotSupported(type));
    }

    private void assertSupported(TypeChecker tc) {
        assertFalse(tc.isNotSupported(type));
    }

    @Before
    public void setup(){
        BDDMockito.given(type.isAnnotationPresent(WithEndoscope.class)).willReturn(false);
    }

    @Test
    public void should_not_support_already_annotated_type(){
        BDDMockito.given(type.isAnnotationPresent(WithEndoscope.class)).willReturn(true);

        TypeChecker tc = new TypeChecker(null, null, ".*");

        assertNotSupported(tc);
    }

    @Test
    public void should_support_type(){
        BDDMockito.given(type.getJavaClass()).willReturn(TheType.class);

        TypeChecker tc = new TypeChecker(null, null, ".*");

        assertSupported(tc);
    }

    @Test
    public void should_include_type_by_package(){
        BDDMockito.given(type.getJavaClass()).willReturn(TheType.class);

        TypeChecker tc = new TypeChecker(new String[]{"org.endoscope"}, null, ".*");

        assertSupported(tc);
    }

    @Test
    public void should_not_include_type_by_package(){
        BDDMockito.given(type.getJavaClass()).willReturn(TheType.class);

        TypeChecker tc = new TypeChecker(new String[]{"org.other"}, null, ".*");

        assertNotSupported(tc);
    }

    @Test
    public void should_not_exclude_type_by_package(){
        BDDMockito.given(type.getJavaClass()).willReturn(TheType.class);

        TypeChecker tc = new TypeChecker(null, new String[]{"org.endoscope.other"}, ".*");

        assertSupported(tc);
    }

    @Test
    public void should_exclude_type_by_package(){
        BDDMockito.given(type.getJavaClass()).willReturn(TheType.class);

        TypeChecker tc = new TypeChecker(null, new String[]{"org.endoscope"}, ".*");

        assertNotSupported(tc);
    }

    @Test
    public void should_exclude_type_by_name(){
        BDDMockito.given(type.getJavaClass()).willReturn(TheType.class);

        TypeChecker tc = new TypeChecker(null, null, "JustThis");
        assertNotSupported(tc);

        //make sure type is supported due to name matching - we rely on it in few more tests
        tc = new TypeChecker(null, null, ".*");
        assertSupported(tc);
    }

    @Test
    public void should_not_exclude_type_by_name(){
        BDDMockito.given(type.getJavaClass()).willReturn(TheType.class);

        TypeChecker tc = new TypeChecker(null, null, "TheType");

        assertSupported(tc);
    }

    @Test
    public void should_include_by_type_annotation(){
        TypeChecker tc = new TypeChecker(null, null, "OtherType");//not by name
        BDDMockito.given(type.getJavaClass()).willReturn(TheType.class);

        for( Class c : TypeChecker.SUPPORTED_ANNOTATIONS ){
            BDDMockito.given(type.isAnnotationPresent(c)).willReturn(true);
            assertSupported(tc);
        }
    }

    @Test
    public void should_not_include_by_type_annotation(){
        TypeChecker tc = new TypeChecker(null, null, "OtherType");//not by name
        BDDMockito.given(type.getJavaClass()).willReturn(TheType.class);

        for( Class c : TypeChecker.SUPPORTED_ANNOTATIONS ){
            BDDMockito.given(type.isAnnotationPresent(c)).willReturn(false);
            assertNotSupported(tc);
        }
    }
}