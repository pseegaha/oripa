/**
 * ORIPA - Origami Pattern Editor 
 * Copyright (C) 2005-2009 Jun Mitani http://mitani.cs.tsukuba.ac.jp/

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package oripa.view.main;

import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import oripa.Config;
import oripa.ORIPA;
import oripa.bind.ButtonFactory;
import oripa.bind.PaintActionButtonFactory;
import oripa.doc.Doc;
import oripa.doc.DocDAO;
import oripa.doc.DocFilterSelector;
import oripa.doc.FileTypeKey;
import oripa.file.AbstractSavingAction;
import oripa.file.FileAccessSupportFilter;
import oripa.file.FileHistory;
import oripa.file.FileVersionError;
import oripa.file.ImageResourceLoader;
import oripa.fold.OrigamiModel;
import oripa.fold.OrigamiModelFactory;
import oripa.paint.CreasePatternInterface;
import oripa.paint.PaintContextInterface;
import oripa.paint.core.PaintConfig;
import oripa.paint.core.PaintContext;
import oripa.paint.cptool.Painter;
import oripa.paint.util.DeleteSelectedLines;
import oripa.resource.Constants;
import oripa.resource.ResourceHolder;
import oripa.resource.ResourceKey;
import oripa.resource.StringID;
import oripa.util.gui.ChildFrameManager;
import oripa.viewsetting.main.MainFrameSettingDB;
import oripa.viewsetting.main.MainScreenSettingDB;

public class MainFrame extends JFrame implements ActionListener,
		ComponentListener, WindowListener, Observer {

	private static final Logger LOGGER = LogManager.getLogger(MainFrame.class);

	/**
	 * 
	 */
	private static final long serialVersionUID = 272369294032419950L;

	private final MainFrameSettingDB setting = MainFrameSettingDB.getInstance();
	private final MainScreenSettingDB screenSetting = MainScreenSettingDB
			.getInstance();
	private final PaintContextInterface mouseContext = PaintContext
			.getInstance();

	PainterScreen mainScreen;
	private final JMenu menuFile = new JMenu(
			ORIPA.res.getString(StringID.Main.FILE_ID));
	private final JMenu menuEdit = new JMenu(ORIPA.res.getString("Edit"));
	private final JMenu menuHelp = new JMenu(ORIPA.res.getString("Help"));
	private final JMenuItem menuItemClear = new JMenuItem(
			ORIPA.res.getString("New"));
	private final JMenuItem menuItemOpen = new JMenuItem(
			ORIPA.res.getString("Open"));

	private final JMenuItem menuItemSave = new JMenuItem(
			ORIPA.res.getString("Save"));
	private final JMenuItem menuItemSaveAs = new JMenuItem(
			ORIPA.res.getString("SaveAs"));
	private final JMenuItem menuItemSaveAsImage = new JMenuItem(
			ORIPA.res.getString("SaveAsImage"));

	private final JMenuItem menuItemExportDXF = new JMenuItem("Export DXF");
	private final JMenuItem menuItemExportOBJ = new JMenuItem("Export OBJ");
	private final JMenuItem menuItemExportCP = new JMenuItem("Export CP");
	private final JMenuItem menuItemExportSVG = new JMenuItem("Export SVG");

	// -----------------------------------------------------------------------------------------------------------
	// Create paint button

	ButtonFactory buttonFactory = new PaintActionButtonFactory();

	/**
	 * For changing outline
	 */
	private final JMenuItem menuItemChangeOutline = (JMenuItem) buttonFactory
			.create(this, JMenuItem.class, StringID.EDIT_CONTOUR_ID);

	/**
	 * For selecting all lines
	 */
	private final JMenuItem menuItemSelectAll = (JMenuItem) buttonFactory
			.create(this, JMenuItem.class, StringID.SELECT_ALL_LINE_ID);

	/**
	 * For starting copy-and-paste
	 */
	private final JMenuItem menuItemCopyAndPaste = (JMenuItem) buttonFactory
			.create(this, JMenuItem.class, StringID.COPY_PASTE_ID);

	/**
	 * For starting cut-and-paste
	 */
	private final JMenuItem menuItemCutAndPaste = (JMenuItem) buttonFactory
			.create(this, JMenuItem.class, StringID.CUT_PASTE_ID);

	// -----------------------------------------------------------------------------------------------------------

	private final ResourceHolder resourceHolder = ResourceHolder.getInstance();
	private final JMenuItem menuItemProperty = new JMenuItem(
			resourceHolder.getString(ResourceKey.LABEL,
					StringID.Main.PROPERTY_ID));

	private final JMenuItem menuItemExit = new JMenuItem(
			resourceHolder.getString(ResourceKey.LABEL, StringID.Main.EXIT_ID));
	private final JMenuItem menuItemUndo = new JMenuItem(
			ORIPA.res.getString("Undo"));
	private final JMenuItem menuItemAbout = new JMenuItem(
			ORIPA.res.getString("About"));
	private final JMenuItem menuItemRepeatCopy = new JMenuItem("Array Copy");
	private final JMenuItem menuItemCircleCopy = new JMenuItem("Circle Copy");
	private final JMenuItem menuItemUnSelectAll = new JMenuItem("UnSelect All");

	private final JMenuItem menuItemDeleteSelectedLines = new JMenuItem(
			"Delete Selected Lines");
	private final JMenuItem[] MRUFilesMenuItem = new JMenuItem[Config.MRUFILE_NUM];

	private RepeatCopyDialog arrayCopyDialog;
	private CircleCopyDialog circleCopyDialog;
	public static JLabel hintLabel = new JLabel();
	public UIPanel uiPanel;

	private final FileHistory fileHistory = new FileHistory(Config.MRUFILE_NUM);

	private final DocFilterSelector filterDB = new DocFilterSelector();
	private final FileAccessSupportFilter[] fileFilters = new FileAccessSupportFilter[] {

			filterDB.getFilter("opx"), filterDB.getFilter("pict") };

	public MainFrame() {

		setting.addObserver(this);

		// addKeyListener(this);

		menuItemCopyAndPaste.setText(resourceHolder.getString(
				ResourceKey.LABEL, StringID.COPY_PASTE_ID));
		menuItemCutAndPaste.setText(resourceHolder.getString(ResourceKey.LABEL,
				StringID.CUT_PASTE_ID));
		// menuItemChangeOutline.setText(ORIPA.res.getString(StringID.Menu.CONTOUR_ID));

		mainScreen = new PainterScreen();
		addWindowListener(this);
		uiPanel = new UIPanel(mainScreen);
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(uiPanel, BorderLayout.WEST);
		getContentPane().add(mainScreen, BorderLayout.CENTER);
		getContentPane().add(hintLabel, BorderLayout.SOUTH);

		ImageResourceLoader imgLoader = new ImageResourceLoader();
		this.setIconImage(imgLoader.loadAsIcon("icon/oripa.gif", getClass())
				.getImage());

		menuItemOpen.addActionListener(this);
		menuItemOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
				ActionEvent.CTRL_MASK));

		menuItemSave.addActionListener(this);
		menuItemSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
				ActionEvent.CTRL_MASK));

		menuItemSaveAs.addActionListener(this);
		menuItemSaveAsImage.addActionListener(this);

		menuItemExit.addActionListener(this);
		// menuItemExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X,
		// ActionEvent.CTRL_MASK));

		menuItemUndo.addActionListener(this);
		menuItemUndo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z,
				ActionEvent.CTRL_MASK));

		menuItemClear.addActionListener(this);
		menuItemClear.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N,
				ActionEvent.CTRL_MASK));

		menuItemAbout.addActionListener(this);
		menuItemExportDXF.addActionListener(this);
		menuItemExportOBJ.addActionListener(this);
		menuItemExportCP.addActionListener(this);
		menuItemExportSVG.addActionListener(this);
		menuItemProperty.addActionListener(this);
		menuItemChangeOutline.addActionListener(this);
		menuItemRepeatCopy.addActionListener(this);
		menuItemCircleCopy.addActionListener(this);
		menuItemSelectAll.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A,
				ActionEvent.CTRL_MASK));

		menuItemUnSelectAll.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_ESCAPE, 0));

		menuItemUnSelectAll
				.addActionListener(new java.awt.event.ActionListener() {
					@Override
					public void actionPerformed(java.awt.event.ActionEvent e) {
						CreasePatternInterface creasePattern = ORIPA.doc
								.getCreasePattern();
						Painter painter = new Painter();
						painter.resetSelectedOriLines(creasePattern);

						mouseContext.clear(false);
						mainScreen.repaint();
					}
				});

		menuItemDeleteSelectedLines
				.addActionListener(new DeleteSelectedLines());
		menuItemDeleteSelectedLines.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_DELETE, 0));

		menuItemCopyAndPaste.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_C, ActionEvent.CTRL_MASK));
		menuItemCutAndPaste.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_X, ActionEvent.CTRL_MASK));

		for (int i = 0; i < Config.MRUFILE_NUM; i++) {
			MRUFilesMenuItem[i] = new JMenuItem();
			MRUFilesMenuItem[i].addActionListener(this);
		}

		loadIniFile();

		// Building the menu bar
		JMenuBar menuBar = new JMenuBar();
		buildMenuFile();

		menuEdit.add(menuItemCopyAndPaste);
		menuEdit.add(menuItemCutAndPaste);
		menuEdit.add(menuItemRepeatCopy);
		menuEdit.add(menuItemCircleCopy);
		menuEdit.add(menuItemSelectAll);
		menuEdit.add(menuItemUnSelectAll);
		menuEdit.add(menuItemDeleteSelectedLines);
		menuEdit.add(menuItemUndo);
		menuEdit.add(menuItemChangeOutline);

		menuHelp.add(menuItemAbout);

		menuBar.add(menuFile);
		menuBar.add(menuEdit);
		menuBar.add(menuHelp);
		setJMenuBar(menuBar);

		addSavingActions();
	}

	@SuppressWarnings("unchecked")
	private void addSavingActions() {

		filterDB.getFilter(FileTypeKey.OPX).setSavingAction(
				new AbstractSavingAction<Doc>(Doc.class) {

					@Override
					public boolean save(Doc data) {
						try {

							saveOpxFile(data, getPath());
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							return false;
						}
						return true;
					}
				});
	}

	private void saveOpxFile(Doc doc, String filePath) {
		DocDAO dao = new DocDAO();
		dao.save(doc, filePath, FileTypeKey.OPX);

		updateMenu(filePath);
		updateTitleText();
		doc.clearChanged();
	}

	public void initialize() {
		arrayCopyDialog = new RepeatCopyDialog(this);
		circleCopyDialog = new CircleCopyDialog(this);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Doc document = ORIPA.doc;
		CreasePatternInterface creasePattern = document.getCreasePattern();

		// Check the last opened files
		for (int i = 0; i < Config.MRUFILE_NUM; i++) {
			if (e.getSource() == MRUFilesMenuItem[i]) {
				try {
					String filePath = MRUFilesMenuItem[i].getText();
					openFile(filePath);
					updateTitleText();
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(this, e.toString(),
							ORIPA.res.getString("Error_FileLoadFailed"),
							JOptionPane.ERROR_MESSAGE);
				}
				mainScreen.repaint();
				return;
			}
		}

		// TODO Refactor the long, long if-else sequences!

		// String lastPath = fileHistory.getLastPath();
		String lastDirectory = fileHistory.getLastDirectory();

		if (e.getSource() == menuItemOpen) {
			openFile(null);
			mainScreen.repaint();
			updateTitleText();
		} else if (e.getSource() == menuItemSave
				&& !document.getDataFilePath().equals("")) {
			saveOpxFile(document, document.getDataFilePath());

		} else if (e.getSource() == menuItemSaveAs
				|| e.getSource() == menuItemSave) {

			String path = saveFile(lastDirectory, document.getDataFileName(),
					filterDB.getSavables());

			updateMenu(path);
			updateTitleText();

		} else if (e.getSource() == menuItemSaveAsImage) {

			saveFile(
					lastDirectory,
					document.getDataFileName(),
					new FileAccessSupportFilter[] { filterDB
							.getFilter(FileTypeKey.PICT) });

		} else if (e.getSource() == menuItemExportDXF) {
			saveModelFile(FileTypeKey.DXF_MODEL);
		} else if (e.getSource() == menuItemExportOBJ) {
			saveModelFile(FileTypeKey.OBJ_MODEL);
		} else if (e.getSource() == menuItemExportCP) {
			saveModelFile(FileTypeKey.CP);
		} else if (e.getSource() == menuItemExportSVG) {
			saveModelFile(FileTypeKey.SVG);
		} else if (e.getSource() == menuItemChangeOutline) {
			// Globals.preEditMode = Globals.editMode;
			// Globals.editMode = Constants.EditMode.EDIT_OUTLINE;

			// Globals.setMouseAction(new EditOutlineAction());

		} else if (e.getSource() == menuItemExit) {
			saveIniFile();
			System.exit(0);
		} else if (e.getSource() == menuItemUndo) {
			if (PaintConfig.getMouseAction() != null) {
				PaintConfig.getMouseAction().undo(mouseContext);
			} else {
				document.loadUndoInfo();
			}
			mainScreen.repaint();
		} else if (e.getSource() == menuItemClear) {
			document.set(new Doc(Constants.DEFAULT_PAPER_SIZE));

			ChildFrameManager manager = ChildFrameManager.getManager();
			manager.closeAllRecursively(this);
			// ORIPA.modelFrame.repaint();
			//
			// ORIPA.modelFrame.setVisible(false);
			// ORIPA.renderFrame.setVisible(false);

			screenSetting.setGridVisible(true);
			screenSetting.notifyObservers();

			repaint();
			// ORIPA.mainFrame.uiPanel.dispGridCheckBox.setSelected(true);
			updateTitleText();
		} else if (e.getSource() == menuItemAbout) {
			JOptionPane.showMessageDialog(this, ORIPA.infoString,
					ORIPA.res.getString("Title"),
					JOptionPane.INFORMATION_MESSAGE);
		} else if (e.getSource() == menuItemProperty) {
			PropertyDialog dialog = new PropertyDialog(this);
			dialog.setValue();
			Rectangle rec = getBounds();
			dialog.setLocation(
					(int) (rec.getCenterX() - dialog.getWidth() / 2),
					(int) (rec.getCenterY() - dialog.getHeight() / 2));
			dialog.setModal(true);
			dialog.setVisible(true);
		} else if (e.getSource() == menuItemRepeatCopy) {
			Painter painter = new Painter();
			if (painter.countSelectedLines(creasePattern) == 0) {
				JOptionPane.showMessageDialog(this, "Select target lines",
						"ArrayCopy", JOptionPane.WARNING_MESSAGE);

			} else {
				arrayCopyDialog.setVisible(true);
			}
		} else if (e.getSource() == menuItemCircleCopy) {
			Painter painter = new Painter();
			if (painter.countSelectedLines(creasePattern) == 0) {
				JOptionPane.showMessageDialog(this, "Select target lines",
						"ArrayCopy", JOptionPane.WARNING_MESSAGE);

			} else {
				circleCopyDialog.setVisible(true);
			}
		}

	}

	public void updateTitleText() {
		Doc document = ORIPA.doc;
		String fileName;
		if ((document.getDataFilePath()).equals("")) {
			fileName = ORIPA.res.getString("DefaultFileName");
		} else {
			fileName = document.getDataFileName();
		}

		setTitle(fileName + " - " + ORIPA.TITLE);
	}

	private String saveFile(String directory, String fileName,
			FileAccessSupportFilter<Doc>[] filters) {

		File givenFile = new File(directory, fileName);

		return saveFile(givenFile.getPath(), filters);
	}

	private String saveFile(String homePath, FileAccessSupportFilter[] filters) {
		Doc document = ORIPA.doc;

		DocDAO dao = new DocDAO();
		return dao.saveWithGUI(document, null, filters, this);

		// if (saver.getPath() == null) {
		// return homePath;
		// }
		//
		// Image cpImage = mainScreen.getCreasePatternImage();
		// if (saver.targetClassMatches(document)) {
		// saver.save(document);
		// } else if (saver.targetClassMatches(cpImage)) {
		// saver.save(cpImage);
		// }

		// return saver.getPath();
	}

	public void saveModelFile(FileTypeKey type) {
		Doc document = ORIPA.doc;
		CreasePatternInterface creasePattern = document.getCreasePattern();
		OrigamiModel origamiModel = document.getOrigamiModel();

		boolean hasModel = origamiModel.hasModel();

		OrigamiModelFactory modelFactory = new OrigamiModelFactory();
		origamiModel = modelFactory.buildOrigami(creasePattern,
				document.getPaperSize(), true);
		document.setOrigamiModel(origamiModel);

		if (type == FileTypeKey.OBJ_MODEL) {

		} else if (!hasModel && !origamiModel.isProbablyFoldable()) {

			JOptionPane.showConfirmDialog(null,
					"Warning: Building a set of polygons from crease pattern "
							+ "was failed.", "Warning", JOptionPane.OK_OPTION,
					JOptionPane.WARNING_MESSAGE);
		}

		DocDAO dao = new DocDAO();

		dao.saveWithGUI(document, null,
				new FileAccessSupportFilter[] { filterDB.getFilter(type) },
				this);

	}

	private void buildMenuFile() {
		menuFile.removeAll();

		menuFile.add(menuItemClear);
		menuFile.add(menuItemOpen);
		menuFile.add(menuItemSave);
		menuFile.add(menuItemSaveAs);
		menuFile.add(menuItemSaveAsImage);
		menuFile.add(menuItemExportDXF);
		menuFile.add(menuItemExportOBJ);
		menuFile.add(menuItemExportCP);
		menuFile.add(menuItemExportSVG);
		menuFile.addSeparator();
		menuFile.add(menuItemProperty);
		menuFile.addSeparator();

		int i = 0;
		for (String path : fileHistory.getHistory()) {
			MRUFilesMenuItem[i].setText(path);
			menuFile.add(MRUFilesMenuItem[i]);

			i++;
		}
		while (i < MRUFilesMenuItem.length) {
			MRUFilesMenuItem[i].setText("");
			i++;
		}

		menuFile.addSeparator();
		menuFile.add(menuItemExit);
	}

	public void updateMenu(String filePath) {

		if (filterDB.getLoadableFilterOf(filePath) == null) {
			return;
		}

		fileHistory.useFile(filePath);

		buildMenuFile();
	}

	/**
	 * if filePath is null, this method opens a dialog to select the target.
	 * otherwise, it tries to read data from the path.
	 * 
	 * @param filePath
	 */
	private void openFile(String filePath) {
		Doc document = ORIPA.doc;

		// ORIPA.modelFrame.setVisible(false);

		ChildFrameManager.getManager().closeAllRecursively(this);
		// ORIPA.renderFrame.setVisible(false);

		screenSetting.setGridVisible(false);
		screenSetting.notifyObservers();

		// ORIPA.mainFrame.uiPanel.dispGridCheckBox.setSelected(false);

		String path = null;

		DocDAO dao = new DocDAO();

		try {
			if (filePath != null) {
				document.set(dao.load(filePath));
				path = document.getDataFilePath();
			} else {
				DocFilterSelector selector = new DocFilterSelector();
				document.set(dao.loadWithGUI(
						fileHistory.getLastPath(), selector.getLoadables(),
						this));
			}
		} catch (FileVersionError e) {
			showVersionErrorDialog();
		}

		if (path == null) {
			path = document.getDataFilePath();
		} else {
			updateMenu(path);

		}
		updateTitleText();

	}

	private void showVersionErrorDialog() {
		JOptionPane.showMessageDialog(this,
				"This file is compatible with a new version. "
						+ "Please obtain the latest version of ORIPA",
				"Failed to load the file", JOptionPane.ERROR_MESSAGE);

	}

	private void saveIniFile() {
		fileHistory.saveToFile(ORIPA.iniFilePath);
	}

	private void loadIniFile() {
		fileHistory.loadFromFile(ORIPA.iniFilePath);
	}

	@Override
	public void componentResized(ComponentEvent arg0) {
	}

	@Override
	public void componentMoved(ComponentEvent arg0) {
	}

	@Override
	public void componentShown(ComponentEvent arg0) {
	}

	@Override
	public void componentHidden(ComponentEvent arg0) {
	}

	@Override
	public void windowOpened(WindowEvent arg0) {
	}

	@Override
	public void windowClosing(WindowEvent arg0) {
		Doc document = ORIPA.doc;

		if (document.isChanged()) {
			// TODO: confirm saving edited opx
			int selected = JOptionPane
					.showConfirmDialog(
							this,
							"The crease pattern has been modified. Would you like to save?",
							"Comfirm to save", JOptionPane.YES_NO_OPTION);
			if (selected == JOptionPane.YES_OPTION) {
				String path = saveFile(fileHistory.getLastDirectory(),
						document.getDataFileName(), fileFilters);
				if (path == null) {

				}
			}
		}

		saveIniFile();
	}

	@Override
	public void windowClosed(WindowEvent arg0) {
	}

	@Override
	public void windowIconified(WindowEvent arg0) {
	}

	@Override
	public void windowDeiconified(WindowEvent arg0) {
	}

	@Override
	public void windowActivated(WindowEvent arg0) {
	}

	@Override
	public void windowDeactivated(WindowEvent arg0) {
	}

	// @Override
	// public void keyTyped(KeyEvent e) {
	// if(e.isControlDown()){
	// screenUpdater.updateScreen();
	// }
	// }
	//
	// @Override
	// public void keyPressed(KeyEvent e) {
	// if(e.isControlDown()){
	// screenUpdater.updateScreen();
	// }
	// }
	//
	// @Override
	// public void keyReleased(KeyEvent e) {
	// if(e.isControlDown()){
	// screenUpdater.updateScreen();
	// }
	//
	// }

	@Override
	public void update(Observable o, Object arg) {
		if (o.toString() == setting.getName()) {
			hintLabel.setText("    " + setting.getHint());
			hintLabel.repaint();
		}
	}
}
