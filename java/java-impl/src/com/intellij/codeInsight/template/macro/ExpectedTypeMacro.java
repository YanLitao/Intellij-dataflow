// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.codeInsight.template.macro;

import com.intellij.codeInsight.ExpectedTypeInfo;
import com.intellij.codeInsight.ExpectedTypesProvider;
import com.intellij.codeInsight.completion.CompletionUtil;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.template.*;
import com.intellij.codeInsight.template.impl.JavaTemplateUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.text.BlockSupport;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;

public final class ExpectedTypeMacro extends Macro {

  @Override
  public String getName() {
    return "expectedType";
  }

  @Override
  public @NotNull String getDefaultValue() {
    return "A";
  }

  @Override
  public Result calculateResult(Expression @NotNull [] params, ExpressionContext context) {
    PsiType[] types = getExpectedTypes(params, context);
    if (types == null || types.length == 0) return null;
    return new PsiTypeResult(types[0], context.getProject());
  }

  @Override
  public LookupElement[] calculateLookupItems(Expression @NotNull [] params, ExpressionContext context) {
    PsiType[] types = getExpectedTypes(params, context);
    if (types == null || types.length < 2) return null;
    Set<LookupElement> set = new LinkedHashSet<>();
    for (PsiType type : types) {
      JavaTemplateUtil.addTypeLookupItem(set, type);
    }
    return set.toArray(LookupElement.EMPTY_ARRAY);
  }

  private static PsiType @Nullable [] getExpectedTypes(Expression[] params, final ExpressionContext context) {
    if (params.length != 0) return null;

    final Project project = context.getProject();
    PsiType[] types = null;

    PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(context.getEditor().getDocument());
    assert file != null;
    final PsiFile fileCopy = (PsiFile)file.copy();
    BlockSupport.getInstance(project).reparseRange(fileCopy, context.getTemplateStartOffset(), context.getTemplateEndOffset(),
                                                   CompletionUtil.DUMMY_IDENTIFIER);
    
    PsiElement element = fileCopy.findElementAt(context.getTemplateStartOffset());

    if (element instanceof PsiIdentifier && element.getParent() instanceof PsiExpression) {
      ExpectedTypeInfo[] infos = ExpectedTypesProvider.getExpectedTypes((PsiExpression)element.getParent(), true);
      if (infos.length > 0){
        types = new PsiType[infos.length];
        for(int i = 0; i < infos.length; i++) {
          ExpectedTypeInfo info = infos[i];
          types[i] = info.getType();
        }
      }
    }

    return types;
  }

  @Override
  public boolean isAcceptableInContext(TemplateContextType context) {
    return context instanceof JavaCodeContextType;
  }

}
