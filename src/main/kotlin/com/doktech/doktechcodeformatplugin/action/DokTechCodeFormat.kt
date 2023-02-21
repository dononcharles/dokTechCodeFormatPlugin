package com.doktech.doktechcodeformatplugin.action

import com.doktech.doktechcodeformatplugin.utils.isContainsDoubleQuote
import com.doktech.doktechcodeformatplugin.utils.isKotlinFileContent
import com.doktech.doktechcodeformatplugin.utils.isVarOrVal
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

class DokTechCodeFormat : AnAction() {

    private val doubleDot = ":"
    private val space = " "
    private val successDialogMessage = "Code successfully refactored and indented"
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
                var startFormattingFromHere = false
                val sb = StringBuilder()

                while (bufferedReader.readLine()?.let { currentLine = it } != null) {
                    // start formatting from class, data class, interface naming
                    if (currentLine.isKotlinFileContent()) {
                        startFormattingFromHere = true
                        parameterRenamingCore(currentLine, sb)
                    } else {
                        if (startFormattingFromHere) {
                            parameterRenamingCore(currentLine, sb)
                        } else {
                            sb.append(currentLine)
                            sb.append("\n")
                        }
                    }
                }

                contents = sb.toString()
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

    private fun parameterRenamingCore(currentLine: String, sb: StringBuilder) {
        if (currentLine.isBlank() || currentLine.isEmpty()) { // empty line
            sb.append(currentLine)
            sb.append("\n")
        } else {
            val currentLineWords = currentLine.split(" ").toMutableList()
            val tempList = mutableListOf<String>()

            currentLineWords.forEachIndexed { index, word ->
                if (word.contains("_")) { // there is word which needs renaming
                    if (word.matches(Regex("^`?[_a-z][a-zA-Z0-9]*`?\$")).not() && currentLineWords[index - 1].isVarOrVal()) {
                        if (word.contains(doubleDot)) { // double dot (:) is the end of the word. ex: request_name:
                            val pN = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, word.substringBefore(doubleDot))
                            val dP = word.substringAfter(doubleDot)
                            val wordRenamed = pN.plus(doubleDot).plus(dP)
                            tempList.add(wordRenamed)
                        } else { // ex: request_name :String
                            val wordRenamed = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, word)
                            tempList.add(wordRenamed)
                        }
                    } else { // contains _ but do not need to be renamed
                        if (currentLineWords[index - 1].isContainsDoubleQuote() && currentLineWords[index + 1].isContainsDoubleQuote()) {
                            tempList.add(word.trimStart().trimEnd())
                        } else {
                            tempList.add(word)
                        }
                    }
                } else { // no renaming needed
                    tempList.add(word)
                }
            }

            sb.append(tempList.joinToString(space))
            sb.append("\n")
        }
    }

    override fun update(e: AnActionEvent) {
        val currentProject = e.project
        val virtualFile: VirtualFile = e.getData(CommonDataKeys.EDITOR)?.document?.let { FileDocumentManager.getInstance().getFile(it) } ?: return
        e.presentation.isEnabledAndVisible = currentProject != null && virtualFile.extension.toString() == "kt"
    }

}