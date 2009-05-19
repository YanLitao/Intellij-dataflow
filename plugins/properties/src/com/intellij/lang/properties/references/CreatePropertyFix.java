package com.intellij.lang.properties.references;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.CodeInsightUtilBase;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.lang.properties.PropertiesBundle;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.undo.UndoUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class CreatePropertyFix implements IntentionAction, LocalQuickFix {
  private static final Logger LOG = Logger.getInstance("#com.intellij.codeInsight.i18n.I18nizeQuickFix");
  private PsiElement myElement;
  private String myKey;
  private List<PropertiesFile> myPropertiesFiles;

  public static final String NAME = PropertiesBundle.message("create.property.quickfix.text");

  public CreatePropertyFix() {
  }

  public CreatePropertyFix(PsiElement element, String key, final List<PropertiesFile> propertiesFiles) {
    myElement = element;
    myKey = key;
    myPropertiesFiles = propertiesFiles;
  }

  @NotNull
  public String getName() {
    return NAME;
  }

  @NotNull
  public String getFamilyName() {
    return getText();
  }

  public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
    PsiElement psiElement = descriptor.getPsiElement();
    if (isAvailable(project, null, null)) {
      invoke(project, null, psiElement.getContainingFile());
    }
  }

  @NotNull
  public String getText() {
    return NAME;
  }

  public boolean isAvailable(@NotNull Project project, @Nullable Editor editor, @Nullable PsiFile file) {
    return myElement.isValid();
  }

  public void invoke(@NotNull final Project project, @Nullable Editor editor, @NotNull PsiFile file) {
    invokeAction(project, file, myElement, myKey, myPropertiesFiles);
  }

  @Nullable
  protected static Pair<String, String> invokeAction(@NotNull final Project project,
                                                     @NotNull PsiFile file,
                                                     @NotNull PsiElement psiElement,
                                                     @Nullable final String suggestedKey,
                                                     @Nullable final List<PropertiesFile> propertiesFiles) {
    final I18nizeQuickFixDialog dialog = new I18nizeQuickFixDialog(
      project,
      file,
      NAME,
      createDefaultCustomization(suggestedKey, propertiesFiles)
    );
    return doAction(project, psiElement, dialog);
  }

  protected static I18nizeQuickFixDialog.DialogCustomization createDefaultCustomization(String suggestedKey, List<PropertiesFile> propertiesFiles) {
    return new I18nizeQuickFixDialog.DialogCustomization(NAME, false, true, propertiesFiles, suggestedKey == null ? "" : suggestedKey);
  }

  protected static Pair<String, String> doAction(Project project, PsiElement psiElement, 
                                               I18nizeQuickFixDialog dialog) {
    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      dialog.show();
      if (!dialog.isOK()) return null;
    }
    final String key = dialog.getKey();
    final String value = dialog.getValue();

    final Collection<PropertiesFile> selectedPropertiesFiles = dialog.getAllPropertiesFiles();
    createProperty(project, psiElement, selectedPropertiesFiles, key, value);

    return new Pair<String, String>(key, value);
  }

  public static void createProperty(@NotNull final Project project,
                                    @NotNull final PsiElement psiElement,
                                    @NotNull final Collection<PropertiesFile> selectedPropertiesFiles,
                                    @NotNull final String key,
                                    @NotNull final String value) {
    for (PropertiesFile selectedFile : selectedPropertiesFiles) {
      if (!CodeInsightUtilBase.prepareFileForWrite(selectedFile)) return;
    }
    UndoUtil.markPsiFileForUndo(psiElement.getContainingFile());

    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      public void run() {
        CommandProcessor.getInstance().executeCommand(project, new Runnable() {
          public void run() {
            try {
              I18nUtil.createProperty(project, selectedPropertiesFiles, key, value);
            }
            catch (IncorrectOperationException e) {
              LOG.error(e);
            }
          }
        }, CodeInsightBundle.message("quickfix.i18n.command.name"), project);
      }
    });
  }

  public boolean startInWriteAction() {
    return false;
  }
}
