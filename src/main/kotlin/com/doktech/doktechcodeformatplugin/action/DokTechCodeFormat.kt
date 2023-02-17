package com.doktech.doktechcodeformatplugin.action

import com.google.common.base.CaseFormat
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.application.ex.ApplicationManagerEx
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.codeStyle.CodeStyleManager
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException
import java.util.*

class DokTechCodeFormat : AnAction() {

    private val doubleDot = ":"
    private val space = " "
    private val successDialogMessage = "Code successfully formatted and indented"
    private val successDialogTitle = "Dok Tech"

    override fun actionPerformed(e: AnActionEvent) {
        val selectedEditor = e.getData(CommonDataKeys.EDITOR)
        val selectedProject = e.getData(CommonDataKeys.PROJECT)
        val selectedDocument = selectedEditor?.document
        val virtualFile: VirtualFile = selectedDocument?.let { FileDocumentManager.getInstance().getFile(it) } ?: return
        if (virtualFile.exists() && virtualFile.isWritable) {
            val contents: String
            try {
                val bufferedReader = BufferedReader(FileReader(virtualFile.path))
                var currentLine = ""
                val sb = StringBuilder()
                while (bufferedReader.readLine()?.let { currentLine = it } != null) {
                    val st = StringTokenizer(currentLine)
                    while (st.hasMoreElements()) {
                        var currentWord = st.nextElement()
                        if (currentWord.toString().contains("_")) { // there is word which need renaming

                            if (currentWord.toString().matches(Regex("^`?[_a-z][a-zA-Z0-9]*`?\$")).not()) {
                                if (currentWord.toString()
                                        .contains(doubleDot)
                                ) { // double dot (:) is the end of the word. ex: request_name:
                                    val pN = CaseFormat.LOWER_UNDERSCORE.to(
                                        CaseFormat.LOWER_CAMEL,
                                        currentWord.toString().substringBefore(":")
                                    )
                                    val dP = currentWord.toString().substringAfter(doubleDot)
                                    currentWord = pN.plus(doubleDot).plus(dP)
                                    sb.append(currentWord)
                                    sb.append(space)
                                } else { // ex: request_name :String
                                    sb.append(currentWord)
                                    sb.append(space)
                                }
                            } else {
                                sb.append(currentWord)
                                sb.append(space)
                            }
                        } else {
                            sb.append(currentWord)
                            sb.append(space)
                        }
                    }
                }
                contents = sb.toString()
                println(contents)
            } catch (e: IOException) {
                return
            }

            WriteCommandAction.runWriteCommandAction(selectedProject) {

                val f = selectedProject?.let { PsiDocumentManager.getInstance(it) }

                f?.commitDocument(selectedDocument.apply { setText(contents) })

                if (selectedProject != null) {
                    val m = CodeStyleManager.getInstance(selectedProject)
                    val psiFile = e.getData(LangDataKeys.PSI_FILE)
                    if (psiFile != null) {
                        m.reformat(psiFile)
                    }
                }
            }

            ApplicationManagerEx
                .getApplication()
                .invokeLater {
                    Messages.showMessageDialog(
                        selectedProject,
                        successDialogMessage,
                        successDialogTitle,
                        Messages.getInformationIcon()
                    )
                }
        }
    }


    override fun update(e: AnActionEvent) {
        val currentProject = e.project
        val currentEditor = e.getData(CommonDataKeys.EDITOR)

        e.presentation.isEnabledAndVisible = currentProject != null && currentEditor != null
    }

}