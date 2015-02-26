package pl.mjedynak.idea.plugins.builder.psi;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.util.IncorrectOperationException;
import org.apache.commons.lang.StringUtils;
import pl.mjedynak.idea.plugins.builder.psi.model.PsiFieldsForBuilder;
import pl.mjedynak.idea.plugins.builder.settings.CodeStyleSettings;

import java.util.List;
import java.util.Locale;

import static com.intellij.openapi.util.text.StringUtil.isVowel;

@SuppressWarnings("PMD.TooManyMethods")
public class BuilderPsiClassBuilder {

    private static final String PRIVATE_STRING = "private";
    private static final String SPACE = " ";
    private static final String A_PREFIX = " a";
    private static final String AN_PREFIX = " an";
    private static final String SEMICOLON = ",";

    private PsiHelper psiHelper;
    private PsiFieldsModifier psiFieldsModifier = new PsiFieldsModifier();
    private CodeStyleSettings codeStyleSettings = new CodeStyleSettings();
    private ButMethodCreator butMethodCreator;
    private MethodCreator methodCreator;

    private Project project = null;
    private PsiDirectory targetDirectory = null;
    private PsiClass srcClass = null;
    private String builderClassName = null;

    private List<PsiField> psiFieldsForSetters = null;
    private List<PsiField> psiFieldsForConstructor = null;

    private PsiClass builderClass = null;
    private PsiElementFactory elementFactory = null;
    private String srcClassName = null;
    private String srcClassFieldName = null;

    public BuilderPsiClassBuilder(PsiHelper psiHelper) {
        this.psiHelper = psiHelper;
    }

    public BuilderPsiClassBuilder aBuilder(Project project, PsiDirectory targetDirectory, PsiClass psiClass, String builderClassName, PsiFieldsForBuilder psiFieldsForBuilder) {
        this.project = project;
        this.targetDirectory = targetDirectory;
        this.srcClass = psiClass;
        this.builderClassName = builderClassName;
        JavaDirectoryService javaDirectoryService = psiHelper.getJavaDirectoryService();
        builderClass = javaDirectoryService.createClass(targetDirectory, builderClassName);
        JavaPsiFacade javaPsiFacade = psiHelper.getJavaPsiFacade(project);
        elementFactory = javaPsiFacade.getElementFactory();
        srcClassName = psiClass.getName();
        srcClassFieldName = StringUtils.uncapitalize(srcClassName);
        psiFieldsForSetters = psiFieldsForBuilder.getFieldsForSetters();
        psiFieldsForConstructor = psiFieldsForBuilder.getFieldsForConstructor();
        methodCreator = new MethodCreator(elementFactory, builderClassName);
        butMethodCreator = new ButMethodCreator(elementFactory);

        return this;
    }

    public BuilderPsiClassBuilder withFields() {
        checkClassFieldsRequiredForBuilding();
        builderClass.add(psiHelper.createField(srcClass, srcClassFieldName));
       // psiFieldsModifier.modifyFields(psiFieldsForSetters, psiFieldsForConstructor, builderClass);
        return this;
    }

    public BuilderPsiClassBuilder withPrivateConstructor() {
        checkClassFieldsRequiredForBuilding();
        PsiMethod constructor = elementFactory.createConstructor();
        constructor.getModifierList().setModifierProperty(PRIVATE_STRING, true);
        String srcClassInitializer = classInitializer();
        insertCodeBlock(elementFactory, constructor, srcClassInitializer);
        builderClass.add(constructor);
        return this;
    }

    private String classInitializer(){
        StringBuilder buildMethodText = new StringBuilder();
        return buildMethodText.append(srcClassFieldName).append(" = new ").append(srcClassName).append("();").toString();
    }

    protected void insertCodeBlock(PsiElementFactory factory, PsiMethod method, String template) {
        //insert code checker into method
        try {
            PsiCodeBlock body = method.getBody();
            PsiElement code = factory.createStatementFromText(template, method);
            if (body.getLBrace() != null) {
                method.addAfter(code, body.getLBrace());
            } else {
                body.add(code);
            }
        } catch (IncorrectOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public BuilderPsiClassBuilder withSetMethods(String methodPrefix) {
        checkClassFieldsRequiredForBuilding();
        for (PsiField psiFieldForSetter : psiFieldsForSetters) {
            createAndAddMethod(psiFieldForSetter, methodPrefix);
        }
        for (PsiField psiFieldForConstructor : psiFieldsForConstructor) {
            createAndAddMethod(psiFieldForConstructor, methodPrefix);
        }
        return this;
    }

    private void createAndAddMethod(PsiField psiField, String methodPrefix) {
        builderClass.add(methodCreator.createMethod(psiField, srcClassFieldName));
    }

    public PsiClass build() {
        checkBuilderField();
        StringBuilder buildMethodText = new StringBuilder();
        buildMethodText.append("public ").append(srcClassName).append(" build() { ");
        buildMethodText.append("return ").append(srcClassFieldName).append(";}");
        PsiMethod buildMethod = elementFactory.createMethodFromText(buildMethodText.toString(), srcClass);
        builderClass.add(buildMethod);
        return builderClass;
    }

    public BuilderPsiClassBuilder withButMethod() {
        //  PsiMethod method = butMethodCreator.butMethod(builderClassName, builderClass, srcClass);
        // builderClass.add(method);
        return this;
    }

    public BuilderPsiClassBuilder withInitializingMethod() {
        checkClassFieldsRequiredForBuilding();
        String prefix = isVowel(srcClassName.toLowerCase(Locale.ENGLISH).charAt(0)) ? AN_PREFIX : A_PREFIX;
        PsiMethod staticMethod = elementFactory.createMethodFromText(
                "public static " + builderClassName + prefix + srcClassName + "() { return new " + builderClassName + "();}", srcClass);
        builderClass.add(staticMethod);
        return this;
    }

    private void removeLastSemicolon(StringBuilder sb) {
        if (sb.toString().endsWith(SEMICOLON)) {
            sb.deleteCharAt(sb.length() - 1);
        }
    }

    private void checkBuilderField() {
        if (builderClass == null) {
            throw new IllegalStateException("Builder field not created. Invoke at least aBuilder() method before.");
        }
    }

    private void checkClassFieldsRequiredForBuilding() {
        if (anyFieldIsNull()) {
            throw new IllegalStateException("Fields not set. Invoke aBuilder() method before.");
        }
    }

    private boolean anyFieldIsNull() {
        return (project == null || targetDirectory == null || srcClass == null || builderClassName == null
                || psiFieldsForSetters == null || psiFieldsForConstructor == null);
    }

}
