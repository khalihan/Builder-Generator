package pl.mjedynak.idea.plugins.builder.psi;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiFieldImpl;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import pl.mjedynak.idea.plugins.builder.psi.model.PsiFieldsForBuilder;
import pl.mjedynak.idea.plugins.builder.settings.CodeStyleSettings;

import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.getField;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@RunWith(MockitoJUnitRunner.class)
public class BuilderPsiClassBuilderTest {

    @InjectMocks private BuilderPsiClassBuilder psiClassBuilder;
    @Mock private CodeStyleSettings settings;
    @Mock private PsiHelper psiHelper;
    @Mock private ButMethodCreator butMethodCreator;
    @Mock private MethodCreator methodCreator;
    @Mock private PsiFieldsModifier psiFieldsModifier;
    @Mock private Project project;
    @Mock private PsiDirectory targetDirectory;
    @Mock private PsiClass srcClass;
    @Mock private JavaDirectoryService javaDirectoryService;
    @Mock private PsiClass builderClass;
    @Mock private JavaPsiFacade javaPsiFacade;
    @Mock private PsiElementFactory elementFactory;
    @Mock private PsiFieldsForBuilder psiFieldsForBuilder;
    @Mock private PsiField srcClassNameField;
    @Mock private PsiMethod psiMethod;

    private List<PsiField> psiFieldsForSetters;
    private List<PsiField> psiFieldsForConstructor;

    private String builderClassName = "BuilderClassName";
    private String srcClassName = "ClassName";
    private String srcClassFieldName = "className";

    @Before
    public void setUp() {
        setField(psiClassBuilder, "psiFieldsModifier", psiFieldsModifier);
        psiFieldsForConstructor = new ArrayList<PsiField>();
        psiFieldsForSetters = new ArrayList<PsiField>();
        given(psiHelper.getJavaDirectoryService()).willReturn(javaDirectoryService);
        given(javaDirectoryService.createClass(targetDirectory, builderClassName)).willReturn(builderClass);
        given(psiHelper.getJavaPsiFacade(project)).willReturn(javaPsiFacade);
        given(javaPsiFacade.getElementFactory()).willReturn(elementFactory);
        given(srcClass.getName()).willReturn(srcClassName);
        given(psiFieldsForBuilder.getFieldsForConstructor()).willReturn(psiFieldsForConstructor);
        given(psiFieldsForBuilder.getFieldsForSetters()).willReturn(psiFieldsForSetters);
        mockCodeStyleManager();
    }

    private void mockCodeStyleManager() {
        setField(psiClassBuilder, "codeStyleSettings", settings);
        given(settings.getFieldNamePrefix()).willReturn("m_");
        given(settings.getParameterNamePrefix()).willReturn(EMPTY);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldSetPassedFieldsAndCreateRequiredOnes() {
        // when
        BuilderPsiClassBuilder result = psiClassBuilder.aBuilder(project, targetDirectory, srcClass, builderClassName, psiFieldsForBuilder);

        // then
        assertThat(result, is(sameInstance(psiClassBuilder)));
        assertThat((Project) getField(psiClassBuilder, "project"), is(project));
        assertThat((PsiDirectory) getField(psiClassBuilder, "targetDirectory"), is(targetDirectory));
        assertThat((PsiClass) getField(psiClassBuilder, "srcClass"), is(srcClass));
        assertThat((String) getField(psiClassBuilder, "builderClassName"), is(builderClassName));
        assertThat((List<PsiField>) getField(psiClassBuilder, "psiFieldsForSetters"), is(psiFieldsForSetters));
        assertThat((List<PsiField>) getField(psiClassBuilder, "psiFieldsForConstructor"), is(psiFieldsForConstructor));
        assertThat((PsiClass) getField(psiClassBuilder, "builderClass"), is(builderClass));
        assertThat((PsiElementFactory) getField(psiClassBuilder, "elementFactory"), is(elementFactory));
        assertThat((String) getField(psiClassBuilder, "srcClassName"), is(srcClassName));
        assertThat((String) getField(psiClassBuilder, "srcClassFieldName"), is(srcClassFieldName));
    }

    @Test
    public void shouldDelegatePsiFieldsModification() {
        // when
        BuilderPsiClassBuilder result = psiClassBuilder.aBuilder(project, targetDirectory, srcClass, builderClassName, psiFieldsForBuilder).withFields();

        // then
        verify(psiFieldsModifier).modifyFields(psiFieldsForSetters, psiFieldsForConstructor, builderClass);
        assertThat(result, is(sameInstance(psiClassBuilder)));
    }

    @Test
    @Ignore
    public void shouldAddPrivateConstructorToBuildClass() {
        // given
        PsiMethod constructor = mock(PsiMethod.class);
        PsiModifierList modifierList = mock(PsiModifierList.class);
        given(constructor.getModifierList()).willReturn(modifierList);
        given(elementFactory.createConstructor()).willReturn(constructor);

        // when
        psiClassBuilder.aBuilder(project, targetDirectory, srcClass, builderClassName, psiFieldsForBuilder).withPrivateConstructor();

        // then
        verify(modifierList).setModifierProperty("private", true);
        verify(builderClass).add(constructor);
        verifyNoMoreInteractions(builderClass);
    }

    @Test
    public void shouldAddInitializingMethod() {
        // given
        PsiMethod method = mock(PsiMethod.class);
        given(elementFactory.createMethodFromText(
                "public static " + builderClassName + " a" + srcClassName + "() { return new " + builderClassName + "();}", srcClass)).willReturn(method);

        // when
        psiClassBuilder.aBuilder(project, targetDirectory, srcClass, builderClassName, psiFieldsForBuilder).withInitializingMethod();

        // then
        verify(builderClass).add(method);
        verifyNoMoreInteractions(builderClass);
    }

    @Test
    public void shouldAddInitializingMethodStartingWithAnIfSourceClassNameStartsWithVowel() {
        // given
        PsiMethod method = mock(PsiMethod.class);
        String srcClassNameStartingWithVowel = "Inventory";
        given(srcClass.getName()).willReturn(srcClassNameStartingWithVowel);
        given(elementFactory.createMethodFromText(
                "public static " + builderClassName + " an" + srcClassNameStartingWithVowel + "() { return new " + builderClassName + "();}", srcClass)).willReturn(method);

        // when
        psiClassBuilder.aBuilder(project, targetDirectory, srcClass, builderClassName, psiFieldsForBuilder).withInitializingMethod();

        // then
        verify(builderClass).add(method);
        verifyNoMoreInteractions(builderClass);
    }

    @Test
    public void shouldAddSetMethodsForFieldsFromBothLists() {
        // given
        PsiFieldImpl psiFieldForSetter = mock(PsiFieldImpl.class);
        psiFieldsForSetters.add(psiFieldForSetter);
        PsiFieldImpl psiFieldForConstructor = mock(PsiFieldImpl.class);
        psiFieldsForConstructor.add(psiFieldForConstructor);
        String methodPrefix = "with";
        PsiMethod methodForFieldForSetter = mock(PsiMethod.class);
        PsiMethod methodForFieldForConstructor = mock(PsiMethod.class);
        given(methodCreator.createMethod(psiFieldForSetter, methodPrefix)).willReturn(methodForFieldForSetter);
        given(methodCreator.createMethod(psiFieldForConstructor, methodPrefix)).willReturn(methodForFieldForConstructor);
        BuilderPsiClassBuilder builder = psiClassBuilder.aBuilder(project, targetDirectory, srcClass, builderClassName, psiFieldsForBuilder);
        setField(builder, "methodCreator", methodCreator);

        // when
        builder.withSetMethods(methodPrefix);

        // then
        verify(builderClass).add(methodForFieldForSetter);
        verify(builderClass).add(methodForFieldForConstructor);
        verifyNoMoreInteractions(builderClass);
    }

    @Test
    public void shouldAddButMethod() {
        // given
        given(butMethodCreator.butMethod(builderClassName, builderClass, srcClass)).willReturn(psiMethod);
        BuilderPsiClassBuilder builder = psiClassBuilder.aBuilder(project, targetDirectory, srcClass, builderClassName, psiFieldsForBuilder);
        setField(builder, "butMethodCreator", butMethodCreator);

        // when
        BuilderPsiClassBuilder result = builder.withButMethod();

        // then
        verify(builderClass).add(psiMethod);
        assertThat(result, is(sameInstance(psiClassBuilder)));
    }

    @Test
    public void shouldReturnBuilderObjectWithBuildMethodUsingSetterAndConstructor() {
        // given
        PsiField psiFieldForSetter = mock(PsiField.class);
        psiFieldsForSetters.add(psiFieldForSetter);

        PsiField psiFieldForConstructor = mock(PsiField.class);
        psiFieldsForConstructor.add(psiFieldForConstructor);
        given(psiFieldForConstructor.getName()).willReturn("age");

        given(psiFieldForSetter.getName()).willReturn("name");
        PsiMethod method = mock(PsiMethod.class);
        given(elementFactory.createMethodFromText("public " + srcClassName + " build() { " + srcClassName + " " + srcClassFieldName + " = new " + srcClassName + "(age);"
                + srcClassFieldName + ".setName(name);return " + srcClassFieldName + ";}", srcClass)).willReturn(method);
        // when
        PsiClass result = psiClassBuilder.aBuilder(project, targetDirectory, srcClass, builderClassName, psiFieldsForBuilder).build();

        // then
        verify(builderClass).add(method);
      //  verifyNoMoreInteractions(builderClass);
        assertThat(result, is(notNullValue()));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionWhenInvokingWithPrivateConstructorIfFieldsNotSetBefore() {
        // when
        psiClassBuilder.withPrivateConstructor();
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionWhenInvokingWithInitializingMethodIfFieldsNotSetBefore() {
        // when
        psiClassBuilder.withInitializingMethod();
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionWhenInvokingWithSetMethodsIfFieldsNotSetBefore() {
        // when
        psiClassBuilder.withSetMethods("with");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionWhenInvokingWithFieldsMethodIfFieldsNotSetBefore() {
        // when
        psiClassBuilder.withFields();
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionWhenInvokingBuildMethodIfFieldsNotSetBefore() {
        // when
        psiClassBuilder.build();
    }
}
