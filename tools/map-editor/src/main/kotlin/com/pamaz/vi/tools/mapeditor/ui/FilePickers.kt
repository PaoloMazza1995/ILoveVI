package com.pamaz.vi.tools.mapeditor.ui

import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/** Plain Swing file dialogs — Compose Desktop has no bundled file picker composable, and this tool doesn't need anything fancier. */
object FilePickers {
    fun pickOpenFile(description: String, vararg extensions: String): File? {
        val chooser = JFileChooser()
        if (extensions.isNotEmpty()) chooser.fileFilter =
            FileNameExtensionFilter(description, *extensions)
        return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
    }

    fun pickSaveFile(description: String, vararg extensions: String): File? {
        val chooser = JFileChooser()
        if (extensions.isNotEmpty()) chooser.fileFilter =
            FileNameExtensionFilter(description, *extensions)
        return if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
    }
}
