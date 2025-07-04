/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2011 GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.sourceforge.ganttproject.action.edit;

import kotlin.Unit;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.chart.ChartSelection;
import net.sourceforge.ganttproject.chart.gantt.ClipboardContentsKt;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.gui.view.GPViewManager;
import net.sourceforge.ganttproject.importer.BufferProject;
import net.sourceforge.ganttproject.importer.BufferProjectImportKt;
import net.sourceforge.ganttproject.resource.HumanResourceMerger;
import net.sourceforge.ganttproject.undo.GPUndoManager;

import java.awt.event.ActionEvent;
import java.util.function.Supplier;

import static net.sourceforge.ganttproject.importer.BufferProjectImportKt.importBufferProject;

public class PasteAction extends GPAction {
  private final GPViewManager myViewmanager;
  private final Supplier<GPUndoManager> myUndoManager;
  private final IGanttProject myProject;
  private final UIFacade myUiFacade;

  public PasteAction(IGanttProject project, UIFacade uiFacade, GPViewManager viewManager, Supplier<GPUndoManager> undoManager) {
    super("paste");
    myViewmanager = viewManager;
    myUndoManager = undoManager;
    myProject = project;
    myUiFacade = uiFacade;
    ClipboardContentsKt.onClipboardChange(isAvailable -> {
      updateAction();
      return Unit.INSTANCE;
    });
  }

  @Override
  public void updateAction() {
    super.updateAction();
    setEnabled(ClipboardContentsKt.hasClipboardData() || !myViewmanager.getSelectedArtefacts().isEmpty());
  }

  @Override
  public void actionPerformed(ActionEvent evt) {
    if (calledFromAppleScreenMenu(evt)) {
      return;
    }
    ChartSelection selection = myViewmanager.getSelectedArtefacts();
    if (!selection.isEmpty()) {
      pasteInternalFlavor(selection);
      //myUiFacade.getViewManager().getActiveView().f
      return;
    }
    var clipboardProject = ClipboardContentsKt.getProjectFromClipboard(new BufferProject(myProject, myUiFacade));
    if (clipboardProject != null) {
      try {
        myUndoManager.get().undoableEdit(getLocalizedName(), () -> pasteExternalDocument(clipboardProject));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    myUiFacade.getActiveChart().focus();
  }

  private void pasteExternalDocument(BufferProject clipboardProject) {
    try {
      HumanResourceMerger.MergeResourcesOption mergeOption = new HumanResourceMerger.MergeResourcesOption();
      mergeOption.setValue(HumanResourceMerger.MergeResourcesOption.NO);
      importBufferProject(myProject, clipboardProject, BufferProjectImportKt.asImportBufferProjectApi(myUiFacade),
          mergeOption, null, false);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void pasteInternalFlavor(final ChartSelection selection) {
    myUndoManager.get().undoableEdit(getLocalizedName(), selection::commitClipboardTransaction);
  }

  @Override
  public PasteAction asToolbarAction() {
    final PasteAction result = new PasteAction(myProject, myUiFacade, myViewmanager, myUndoManager);
    result.setFontAwesomeLabel(UIUtil.getFontawesomeLabel(result));
    this.addPropertyChangeListener(evt -> {
      if ("enabled".equals(evt.getPropertyName())) {
        result.setEnabled((Boolean) evt.getNewValue());
      }
    });
    result.setEnabled(this.isEnabled());
    return result;
  }
}
