package pl.mjedynak.idea.plugins.builder.psi;

import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import org.apache.commons.lang.StringUtils;
import pl.mjedynak.idea.plugins.builder.settings.CodeStyleSettings;

public class MethodCreator {

    private CodeStyleSettings codeStyleSettings = new CodeStyleSettings();
    private MethodNameCreator methodNameCreator = new MethodNameCreator();
    private PsiElementFactory elementFactory;
    private String builderClassName;

    public MethodCreator(PsiElementFactory elementFactory, String builderClassName) {
        this.elementFactory = elementFactory;
        this.builderClassName = builderClassName;
    }

    public PsiMethod createMethod(PsiField psiField, String srcClassFieldName) {
        String fieldName = psiField.getName();
        String fieldType = psiField.getType().getPresentableText();
        String fieldNamePrefix = codeStyleSettings.getFieldNamePrefix();
        String fieldNameWithoutPrefix = fieldName.replaceFirst(fieldNamePrefix, "");

        String methodName = fieldNameWithoutPrefix;//methodNameCreator.createMethodName(methodPrefix, fieldNameWithoutPrefix);
        String fieldNameUppercase = StringUtils.capitalize(fieldNameWithoutPrefix);
        StringBuilder buildMethodText = new StringBuilder();
        buildMethodText.append(srcClassFieldName).append(".set").append(fieldNameUppercase).append("(").append(fieldName).append(");");

        String methodText = "public " + builderClassName + " " + methodName + "(" + fieldType + " " + fieldName + ") {"
                + buildMethodText.toString() +
                "return this; }";
        return elementFactory.createMethodFromText(methodText, psiField);
    }
}
