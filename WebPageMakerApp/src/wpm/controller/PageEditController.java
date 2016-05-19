package wpm.controller;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.web.WebEngine;
import properties_manager.PropertiesManager;
import saf.ui.AppMessageDialogSingleton;
import saf.ui.AppYesNoCancelDialogSingleton;
import static wpm.PropertyType.ADD_ELEMENT_ERROR_MESSAGE;
import static wpm.PropertyType.ADD_ELEMENT_ERROR_TITLE;
import static wpm.PropertyType.ATTRIBUTE_UPDATE_ERROR_MESSAGE;
import static wpm.PropertyType.ATTRIBUTE_UPDATE_ERROR_TITLE;
import static wpm.PropertyType.CSS_EXPORT_ERROR_MESSAGE;
import static wpm.PropertyType.CSS_EXPORT_ERROR_TITLE;
import wpm.WebPageMaker;
import wpm.data.DataManager;
import wpm.data.HTMLTagPrototype;
import wpm.file.FileManager;
import static wpm.file.FileManager.TEMP_CSS_PATH;
import static wpm.file.FileManager.TEMP_PAGE;
import wpm.gui.Workspace;

/**
 * This class provides event programmed responses to workspace interactions for
 * this application for things like adding elements, removing elements, and
 * editing them.
 *
 * @author Richard McKenna
 * @author ?
 * @version 1.0
 */
public class PageEditController {

    // HERE'S THE FULL APP, WHICH GIVES US ACCESS TO OTHER STUFF
    WebPageMaker app;

    // WE USE THIS TO MAKE SURE OUR PROGRAMMED UPDATES OF UI
    // VALUES DON'T THEMSELVES TRIGGER EVENTS
    private boolean enabled;

    /**
     * Constructor for initializing this object, it will keep the app for later.
     *
     * @param initApp The JavaFX application this controller is associated with.
     */
    public PageEditController(WebPageMaker initApp) {
	// KEEP IT FOR LATER
	app = initApp;
    }

    /**
     * This mutator method lets us enable or disable this controller.
     *
     * @param enableSetting If false, this controller will not respond to
     * workspace editing. If true, it will.
     */
    public void enable(boolean enableSetting) {
	enabled = enableSetting;
    }

    /**
     * This function responds live to the user typing changes into a text field
     * for updating element attributes. It will respond by updating the
     * appropriate data and then forcing an update of the temp site and its
     * display.
     *
     * @param selectedTag The element in the DOM (our tree) that's currently
     * selected and therefore is currently having its attribute updated.
     *
     * @param attributeName The name of the attribute for the element that is
     * currently being updated.
     *
     * @param attributeValue The new value for the attribute that is being
     * updated.
     */
    public boolean checkImg = false;
    
    public void handleAttributeUpdate(HTMLTagPrototype selectedTag, String attributeName, String attributeValue) {
	if (enabled) {
            try {
                app.getGUI().updateToolbarControls(false);
		// FIRST UPDATE THE ELEMENT'S DATA
		selectedTag.addAttribute(attributeName, attributeValue);
                
                //System.out.println("selectedTag : " + selectedTag.getAttribute("src"));

                /**
                File exportTemp = new File(selectedTag.getAttribute("src"));
                if (!exportTemp.exists() && checkImg == false) {
                    //exportTemp.mkdir();
                    checkImg = true;
                }else if(!exportTemp.exists() && checkImg == true){
        
                    AppMessageDialogSingleton dialog = AppMessageDialogSingleton.getSingleton();
                    dialog.show("Warning", "Do not delete the image.");
                }else{
                    //System.out.println("2. CSS already exists" );
                }        
                */
		// THEN FORCE THE CHANGES TO THE TEMP HTML PAGE
		FileManager fileManager = (FileManager) app.getFileComponent();
		fileManager.exportData(app.getDataComponent(), TEMP_PAGE);

		// AND FINALLY UPDATE THE WEB PAGE DISPLAY USING THE NEW VALUES
		Workspace workspace = (Workspace) app.getWorkspaceComponent();
		workspace.getHTMLEngine().reload();
	    } catch (IOException ioe) {
		// AN ERROR HAPPENED WRITING TO THE TEMP FILE, NOTIFY THE USER
		PropertiesManager props = PropertiesManager.getPropertiesManager();
		AppMessageDialogSingleton dialog = AppMessageDialogSingleton.getSingleton();
		dialog.show(props.getProperty(ATTRIBUTE_UPDATE_ERROR_TITLE), props.getProperty(ATTRIBUTE_UPDATE_ERROR_MESSAGE));
	    }
	}
    }

    /**
     * This function responds to when the user tries to add an element to the
     * tree being edited.
     *
     * @param element The element to add to the tree.
     */
    public void handleAddElementRequest(HTMLTagPrototype element) {
        
        try{     
	if (enabled) {
	    Workspace workspace = (Workspace) app.getWorkspaceComponent();

	    // GET THE TREE TO SEE WHICH NODE IS CURRENTLY SELECTED
	    TreeView tree = workspace.getHTMLTree();
	    TreeItem selectedItem = (TreeItem) tree.getSelectionModel().getSelectedItem();
	    HTMLTagPrototype selectedTag = (HTMLTagPrototype) selectedItem.getValue();
            
            ArrayList<String> legalParents = element.getLegalParents();
            String checkName = selectedTag.getTagName();
            boolean passed = false;
            
            for(int i = 0; i<legalParents.size(); i++){
                if(checkName.equals(legalParents.get(i)))
                    passed = true;
            }
            
            if(passed){
                // MAKE A NEW HTMLTagPrototype AND PUT IT IN A NODE
                HTMLTagPrototype newTag = element.clone();
                TreeItem newNode = new TreeItem(newTag);

                // ADD THE NEW NODE
                selectedItem.getChildren().add(newNode);

                // SELECT THE NEW NODE
                tree.getSelectionModel().select(newNode);
                selectedItem.setExpanded(true);
                
                app.getGUI().updateToolbarControls(false);
                // FORCE A RELOAD OF TAG EDITOR
                workspace.reloadWorkspace();
            }else{
                System.out.println("[illegal parents]");
            }
	    try {
		FileManager fileManager = (FileManager) app.getFileComponent();
		fileManager.exportData(app.getDataComponent(), TEMP_PAGE);
	    } catch (IOException ioe) {
		// AN ERROR HAPPENED WRITING TO THE TEMP FILE, NOTIFY THE USER
		PropertiesManager props = PropertiesManager.getPropertiesManager();
		AppMessageDialogSingleton dialog = AppMessageDialogSingleton.getSingleton();
		dialog.show(props.getProperty(ADD_ELEMENT_ERROR_TITLE), props.getProperty(ADD_ELEMENT_ERROR_MESSAGE));
	    }
	}
        }catch(NullPointerException e){
            System.out.println("NullPointerException");
        }
    }
    
    /**
     * This function responds to when the user tries to remove an element to the
     * tree being edited.
     *
     * @param element The element to remove to the tree.
     */
    public void handleRemoveElementRequest() {
        try{
            if (enabled) {
                Workspace workspace = (Workspace) app.getWorkspaceComponent();

                // GET THE TREE TO SEE WHICH NODE IS CURRENTLY SELECTED
                TreeView tree = workspace.getHTMLTree();
                TreeItem selectedItem = (TreeItem) tree.getSelectionModel().getSelectedItem();
                HTMLTagPrototype selectedTag = (HTMLTagPrototype) selectedItem.getValue();

                String checkName = "";
                checkName = selectedTag.getTagName();

                if(checkName.equals("html") || checkName.equals("head") 
                        || checkName.equals("body") || checkName.equals("title") || checkName.equals("link")){
                    System.out.println(checkName + "is not possible to remove.");
                }else{
                    PropertiesManager props = PropertiesManager.getPropertiesManager();
                    AppYesNoCancelDialogSingleton yesNoDialog = AppYesNoCancelDialogSingleton.getSingleton();
                    yesNoDialog.show("Warning Remove Tag", "Are you really want to remove this tag?");
                    

                    // AND NOW GET THE USER'S SELECTION
                    String selection = yesNoDialog.getSelection();

                    // IF THE USER SAID YES, THEN SAVE BEFORE MOVING ON
                    if (selection.equals(AppYesNoCancelDialogSingleton.YES)) {
                        selectedItem.getParent().getChildren().remove(selectedItem);

                    } // IF THE USER SAID CANCEL, THEN WE'LL TELL WHOEVER
                    // CALLED THIS THAT THE USER IS NOT INTERESTED ANYMORE
                    //(selection.equals(AppYesNoCancelDialogSingleton.CANCEL))
                    
                }
                // FORCE A RELOAD OF TAG EDITOR
                workspace.reloadWorkspace();
                
            } else {
            }

            try {
                FileManager fileManager = (FileManager) app.getFileComponent();
                fileManager.exportData(app.getDataComponent(), TEMP_PAGE);
            } catch (IOException ioe) {
                // AN ERROR HAPPENED WRITING TO THE TEMP FILE, NOTIFY THE USER
                PropertiesManager props = PropertiesManager.getPropertiesManager();
                AppMessageDialogSingleton dialog = AppMessageDialogSingleton.getSingleton();
                dialog.show(props.getProperty(ADD_ELEMENT_ERROR_TITLE), props.getProperty(ADD_ELEMENT_ERROR_MESSAGE));
            }
        }catch(NullPointerException e){
            System.out.println("NullPointerException");
        }
    }
    

    /**
     * This function provides a response to when the user changes the CSS
     * content. It responds but updating the data manager with the new CSS text,
     * and by exporting the CSS to the temp css file.
     *
     * @param cssContent The css content.
     *
     */
    public void handleCSSEditing(String cssContent) {
	if (enabled) {
	    try {
                app.getGUI().updateToolbarControls(false);
		// MAKE SURE THE DATA MANAGER GETS THE CSS TEXT
		DataManager dataManager = (DataManager) app.getDataComponent();
		dataManager.setCSSText(cssContent);

		// WRITE OUT THE TEXT TO THE CSS FILE
		FileManager fileManager = (FileManager) app.getFileComponent();
		fileManager.exportCSS(cssContent, TEMP_CSS_PATH);

		// REFRESH THE HTML VIEW VIA THE ENGINE
		Workspace workspace = (Workspace) app.getWorkspaceComponent();
		WebEngine htmlEngine = workspace.getHTMLEngine();
		htmlEngine.reload();
	    } catch (IOException ioe) {
		AppMessageDialogSingleton dialog = AppMessageDialogSingleton.getSingleton();
		PropertiesManager props = PropertiesManager.getPropertiesManager();
		dialog.show(props.getProperty(CSS_EXPORT_ERROR_TITLE), props.getProperty(CSS_EXPORT_ERROR_MESSAGE));
	    }
	}
    }
}
